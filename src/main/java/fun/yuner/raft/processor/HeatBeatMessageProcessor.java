/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package fun.yuner.raft.processor;

import fun.yuner.raft.RaftCore;
import fun.yuner.raft.bean.RaftStatus;
import fun.yuner.raft.message.HeatBeatMessage;
import fun.yuner.raft.remoting.netty.NettyRequestProcessor;
import fun.yuner.raft.remoting.protocol.RemotingCommand;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;

import java.util.Random;
import java.util.concurrent.*;


@Slf4j
public class HeatBeatMessageProcessor implements NettyRequestProcessor {

    private ScheduledExecutorService heatBeatTimeoutScheduledExecutor = Executors.newSingleThreadScheduledExecutor();
    private int initialDelay = new Random().nextInt(10);
    private RaftCore raftCore;
    private long heatBeatTime;

    public HeatBeatMessageProcessor(RaftCore raftCore) {
        this.raftCore = raftCore;
        heatBeatTimeoutScheduledExecutor.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                if (raftCore.getCurrentStatus() == RaftStatus.LEADER) {
                    return;
                }
                if (System.currentTimeMillis() - heatBeatTime > 10 * 1000) {
                    log.info("heart beat timeout");
                    raftCore.transferStatus(RaftStatus.CANDIDATE);
                }
            }
        }, initialDelay, 10, TimeUnit.SECONDS);
    }

    @Override
    public RemotingCommand processRequest(ChannelHandlerContext ctx, RemotingCommand request) throws Exception {
        String leaderAddress = ctx.channel().remoteAddress().toString();
        log.info("received heart beat from {}", leaderAddress);
        HeatBeatMessage heatBeatMessage = HeatBeatMessage.fromBytes(request.getBody());
        if (heatBeatMessage.getTerm() < raftCore.getCurrentTerm()) {
            return null;
        }
        if (heatBeatMessage.getMirror().equals(raftCore.getMirror())) {
            return null;
        }
        heatBeatTime = System.currentTimeMillis();
        raftCore.transferStatus(RaftStatus.FOLLOWER);
        return null;
    }

    @Override
    public boolean rejectRequest() {
        return false;
    }
}

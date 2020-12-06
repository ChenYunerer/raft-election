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

import com.alibaba.fastjson.JSON;
import fun.yuner.raft.RaftCore;
import fun.yuner.raft.bean.RaftStatus;
import fun.yuner.raft.message.VoteReqMessage;
import fun.yuner.raft.message.VoteResMessage;
import fun.yuner.raft.remoting.netty.NettyRequestProcessor;
import fun.yuner.raft.remoting.protocol.RemotingCommand;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
public class VoteMessageProcessor implements NettyRequestProcessor {

    private RaftCore raftCore;
    private Map<Integer, Boolean> termVoteResult = new ConcurrentHashMap<>();

    public VoteMessageProcessor(RaftCore raftCore) {
        this.raftCore = raftCore;
    }

    @Override
    public synchronized RemotingCommand processRequest(ChannelHandlerContext ctx, RemotingCommand request) throws Exception {
        log.info("receive vote req from {}", ctx.channel().remoteAddress());
        VoteReqMessage voteReqMessage = VoteReqMessage.fromBytes(request.getBody());
        log.info("receive vote req vote term {}", voteReqMessage.getTerm());
        VoteResMessage voteResMessage = new VoteResMessage();
        if (voteReqMessage.getTerm() < raftCore.getCurrentTerm()) {
            voteResMessage.setVote(false);
            voteResMessage.setTerm(raftCore.getCurrentTerm());
        } else if (voteReqMessage.getTerm() == raftCore.getCurrentTerm()) {
            if (termVoteResult.get(voteReqMessage.getTerm()) != null) {
                voteResMessage.setVote(false);
            } else {
                voteResMessage.setVote(true);
                termVoteResult.put(raftCore.getCurrentTerm(), true);
            }
        } else {
            voteResMessage.setVote(true);
            raftCore.setCurrentTerm(voteReqMessage.getTerm());
            termVoteResult.put(raftCore.getCurrentTerm(), true);
        }
        RemotingCommand voteResponse = RemotingCommand.createRequestCommand(RequestCode.VOTE_RES_MESSAGE_CODE);
        voteResponse.setBody(voteResMessage.toBytes());
        log.info("vote to {} voteMsg: {}", ctx.channel().remoteAddress(), voteResMessage);
        return voteResponse;
    }

    @Override
    public boolean rejectRequest() {
        return false;
    }
}

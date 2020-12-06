package fun.yuner.raft;

import fun.yuner.raft.bean.PeerNode;
import fun.yuner.raft.bean.RaftStatus;
import fun.yuner.raft.message.HeatBeatMessage;
import fun.yuner.raft.message.VoteReqMessage;
import fun.yuner.raft.message.VoteResMessage;
import fun.yuner.raft.processor.HeatBeatMessageProcessor;
import fun.yuner.raft.processor.RequestCode;
import fun.yuner.raft.processor.VoteMessageProcessor;
import fun.yuner.raft.remoting.InvokeCallback;
import fun.yuner.raft.remoting.exception.RemotingConnectException;
import fun.yuner.raft.remoting.exception.RemotingSendRequestException;
import fun.yuner.raft.remoting.exception.RemotingTimeoutException;
import fun.yuner.raft.remoting.exception.RemotingTooMuchRequestException;
import fun.yuner.raft.remoting.netty.*;
import fun.yuner.raft.remoting.protocol.RemotingCommand;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;

@Slf4j
public class RaftCore {
    private PeerManager peerManager;
    private RaftConfig raftConfig;
    private volatile RaftStatus currentStatus = RaftStatus.FOLLOWER;
    private volatile int currentTerm;
    private String mirror;
    private NettyRemotingServer remotingServer;
    private NettyRemotingClient remotingClient;
    private ScheduledExecutorService heatBeatScheduledExecutor = Executors.newSingleThreadScheduledExecutor();
    private ScheduledExecutorService voteScheduledExecutor = Executors.newSingleThreadScheduledExecutor();

    public RaftCore(PeerManager peerManager, RaftConfig raftConfig) {
        this.peerManager = peerManager;
        this.raftConfig = raftConfig;
        init();
    }

    private void init(){
        mirror = UUID.randomUUID().toString();
        heatBeatScheduledExecutor.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                if (getCurrentStatus() != RaftStatus.LEADER) {
                    return;
                }
                try {
                    sendHeartBeatMessage2All();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }, raftConfig.getHeartBeatPeriod(), raftConfig.getHeartBeatPeriod(), TimeUnit.SECONDS);
        heatBeatScheduledExecutor.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                if (getCurrentStatus() != RaftStatus.CANDIDATE) {
                    return;
                }
                try {
                    increaseCurrentTerm();
                    sendVoteReqMessage2All();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }, raftConfig.getVoteTimeout(), raftConfig.getVoteTimeout(), TimeUnit.SECONDS);
    }

    public void start() {
        startServer();
        startClient();
    }

    public void startServer(){
        ExecutorService executorService = new ThreadPoolExecutor(6, 12,10, TimeUnit.SECONDS, new ArrayBlockingQueue<>(1000));
        NettyServerConfig nettyServerConfig = new NettyServerConfig();
        nettyServerConfig.setListenPort(raftConfig.getRaftPort());
        remotingServer = new NettyRemotingServer(nettyServerConfig);
        remotingServer.registerProcessor(RequestCode.HEART_BEAT_MESSAGE_CODE, new HeatBeatMessageProcessor(this), executorService);
        remotingServer.registerProcessor(RequestCode.VOTE_REQ_MESSAGE_CODE, new VoteMessageProcessor(this), executorService);

        remotingServer.start();
    }

    public void startClient(){
        NettyClientConfig nettyClientConfig = new NettyClientConfig();
        remotingClient = new NettyRemotingClient(nettyClientConfig);
        remotingClient.start();
    }

    public void sendHeartBeatMessage2All() throws InterruptedException, RemotingSendRequestException, RemotingTimeoutException, RemotingTooMuchRequestException, RemotingConnectException {
        List<PeerNode> peerNodeList = peerManager.getPeerNodeList();
        for (PeerNode peerNode : peerNodeList) {
            RemotingCommand heatBeatRc = RemotingCommand.createRequestCommand(RequestCode.HEART_BEAT_MESSAGE_CODE);
            HeatBeatMessage heatBeatMessage = new HeatBeatMessage();
            heatBeatMessage.setMirror(getMirror());
            heatBeatMessage.setTerm(getCurrentTerm());
            heatBeatRc.setBody(heatBeatMessage.toBytes());
            log.info("send heat beat to {}", peerNode.getAddress());
            remotingClient.invokeOneway(peerNode.getAddress(), heatBeatRc, 10 * 1000);
        }
    }

    public synchronized RaftStatus getCurrentStatus() {
        return currentStatus;
    }

    public String getMirror(){
        return mirror;
    }

    public int getCurrentTerm() {
        return currentTerm;
    }

    public void setCurrentTerm(int currentTerm) {
        this.currentTerm = currentTerm;
    }

    public synchronized int increaseCurrentTerm() {
        return currentTerm++;
    }

    public void transferStatus(RaftStatus to){
        log.info("transfer raft status to {}", to);
        if (currentStatus == to) {
            return;
        }
        currentStatus = to;
        switch (to) {
            case FOLLOWER:
                break;
            case CANDIDATE:
                break;
            case LEADER:
                break;
        }
    }

    private void sendVoteReqMessage2All() throws InterruptedException, RemotingSendRequestException, RemotingTimeoutException, RemotingTooMuchRequestException, RemotingConnectException {
        List<PeerNode> peerNodeList = peerManager.getPeerNodeList();
        final double needVoteNum = peerNodeList.size() * 1.0d / 2;
        final int[] voteNum = {0};
        for (PeerNode peerNode : peerNodeList) {
            VoteReqMessage voteReqMessage = new VoteReqMessage();
            voteReqMessage.setTerm(getCurrentTerm());
            voteReqMessage.setMirror(getMirror());
            RemotingCommand voteRequest = RemotingCommand.createRequestCommand(RequestCode.VOTE_REQ_MESSAGE_CODE);
            voteRequest.setBody(voteReqMessage.toBytes());
            log.info("send vote req to {}", peerNode.getAddress());
            remotingClient.invokeAsync(peerNode.getAddress(), voteRequest, 10 * 1000, new InvokeCallback() {
                @Override
                public void operationComplete(ResponseFuture responseFuture) {
                    log.info("received vote res from {}", responseFuture.getProcessChannel().remoteAddress());
                    if (currentStatus != RaftStatus.CANDIDATE) {
                        return;
                    }
                    RemotingCommand remotingCommand = responseFuture.getResponseCommand();
                    VoteResMessage voteResMessage = VoteResMessage.fromBytes(remotingCommand.getBody());
                    log.info("received vote res {}", voteResMessage);
                    if (voteResMessage.isVote()) {
                        voteNum[0]++;
                    }
                    if (voteNum[0] > needVoteNum) {
                        transferStatus(RaftStatus.LEADER);
                    }
                }
            });
        }
    }
}

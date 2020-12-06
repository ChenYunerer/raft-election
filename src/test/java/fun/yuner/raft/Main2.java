package fun.yuner.raft;

import fun.yuner.raft.bean.PeerNode;

public class Main2 {

    public static void main(String[] arg) {
        PeerManager peerManager = new PeerManager();
        peerManager.addPeer(new PeerNode("127.0.0.1", 8888));
        peerManager.addPeer(new PeerNode("127.0.0.1", 9999));
        RaftConfig raftConfig = new RaftConfig();
        raftConfig.setRaftPort(9999);
        new RaftCore(peerManager, raftConfig).start();
    }
}

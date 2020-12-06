package fun.yuner.raft;

import fun.yuner.raft.bean.PeerNode;

import java.util.ArrayList;
import java.util.List;

public class PeerManager {
    private List<PeerNode> peerNodeList = new ArrayList<>();

    public List<PeerNode> getPeerNodeList() {
        return peerNodeList;
    }

    public void addPeer(PeerNode peerNode) {
        peerNodeList.add(peerNode);
    }
}

package fun.yuner.raft.bean;

public enum RaftStatus {

    LEADER(0), CANDIDATE(1), FOLLOWER(2);

    private int status;

    RaftStatus(int status) {
        this.status = status;
    }

    public int getStatus() {
        return status;
    }
}

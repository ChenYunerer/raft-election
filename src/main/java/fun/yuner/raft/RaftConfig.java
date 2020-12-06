package fun.yuner.raft;

import lombok.Data;

@Data
public class RaftConfig {
    private int raftPort = 8888;
    private int heartBeatPeriod = 10;
    private int voteTimeout = 10;
}

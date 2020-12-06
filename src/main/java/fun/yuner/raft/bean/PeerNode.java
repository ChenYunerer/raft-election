package fun.yuner.raft.bean;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PeerNode {
    private String ip;
    private int port;

    public String getAddress(){
        return ip + ":" + port;
    }
}

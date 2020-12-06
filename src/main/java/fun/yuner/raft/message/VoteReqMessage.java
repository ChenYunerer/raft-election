package fun.yuner.raft.message;

import com.alibaba.fastjson.JSON;
import lombok.Data;

@Data
public class VoteReqMessage extends BaseMessage{

    public static VoteReqMessage fromBytes(byte[] bytes) {
        return JSON.parseObject(new String(bytes), VoteReqMessage.class);
    }
}

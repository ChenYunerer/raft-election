package fun.yuner.raft.message;

import com.alibaba.fastjson.JSON;
import lombok.Data;

@Data
public class VoteResMessage extends BaseMessage{
    private boolean vote;

    public static VoteResMessage fromBytes(byte[] bytes) {
        return JSON.parseObject(new String(bytes), VoteResMessage.class);
    }
}

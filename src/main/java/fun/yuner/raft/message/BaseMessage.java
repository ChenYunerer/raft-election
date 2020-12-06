package fun.yuner.raft.message;

import com.alibaba.fastjson.JSON;
import lombok.Data;

@Data
public abstract class BaseMessage {
    protected int term;
    protected String mirror;

    public byte[] toBytes(){
        String str = JSON.toJSONString(this);
        return str.getBytes();
    }

    public static BaseMessage fromBytes(byte[] bytes) {
        return null;
    }
}

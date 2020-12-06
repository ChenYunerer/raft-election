package fun.yuner.raft.message;

import com.alibaba.fastjson.JSON;
import lombok.Data;

@Data
public class HeatBeatMessage extends BaseMessage{

    public static HeatBeatMessage fromBytes(byte[] bytes) {
        return JSON.parseObject(new String(bytes), HeatBeatMessage.class);
    }
}

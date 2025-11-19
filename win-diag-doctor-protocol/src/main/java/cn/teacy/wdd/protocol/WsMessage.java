package cn.teacy.wdd.protocol;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.concurrent.atomic.AtomicLong;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class WsMessage<P extends WsMessagePayload> {

    private static final AtomicLong MSG_COUNTER = new AtomicLong(1);

    /**
     * 消息标识，自动生成，用于调试
     */
    private String mid;

    /**
     * 消息类型
     */
    private String identifier;

    /**
     * 消息数据
     */
    private P payload;

    public WsMessage(P payload) {
        WsProtocol annotation = payload.getClass().getAnnotation(WsProtocol.class);

        if (annotation == null) {
            throw new IllegalArgumentException(
                    String.format("Class [%s] is missing @WsMsg annotation", payload.getClass().getSimpleName())
            );
        }

        this.identifier = annotation.identifier();
        this.mid = generateMid();
        this.payload = payload;
    }

    private static String generateMid() {
        return String.format("m-%05d", MSG_COUNTER.getAndIncrement());
    }

}

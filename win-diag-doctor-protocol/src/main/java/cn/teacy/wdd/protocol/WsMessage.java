package cn.teacy.wdd.protocol;

import cn.teacy.wdd.protocol.exception.ProtocolAnnotationAbsenceException;
import cn.teacy.wdd.protocol.factory.WsIdGeneratorFactory;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class WsMessage<P extends WsMessagePayload> {

    /**
     * 消息标识，自动生成，用于调试
     */
    private String mid;

    /**
     * 任务标识，用于关联请求和响应
     */
    private String taskId;

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
            throw new ProtocolAnnotationAbsenceException(payload.getClass());
        }

        this.identifier = annotation.identifier();
        this.taskId = WsIdGeneratorFactory.getInstance().generateTaskId();
        this.mid = WsIdGeneratorFactory.getInstance().generateMessageId();
        this.payload = payload;
    }

    public WsMessage(String taskId, P payload) {
        WsProtocol annotation = payload.getClass().getAnnotation(WsProtocol.class);

        if (annotation == null) {
            throw new ProtocolAnnotationAbsenceException(payload.getClass());
        }

        this.identifier = annotation.identifier();
        this.taskId = taskId != null ? taskId : WsIdGeneratorFactory.getInstance().generateTaskId();
        this.mid = WsIdGeneratorFactory.getInstance().generateMessageId();
        this.payload = payload;
    }

}

package cn.teacy.wdd.protocol.factory;

public interface WsMessageIdGenerator {

    /**
     * 生成消息 ID
     */
    String generateMessageId();

    /**
     * 生成任务 ID
     */
    String generateTaskId();

}

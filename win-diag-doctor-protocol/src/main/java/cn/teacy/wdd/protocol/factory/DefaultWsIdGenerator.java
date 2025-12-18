package cn.teacy.wdd.protocol.factory;


import cn.teacy.wdd.common.interfaces.StringIdGenerator;

public class DefaultWsIdGenerator implements WsMessageIdGenerator {

    private final StringIdGenerator messageIdGenerator = new StringIdGenerator.DefaultIdGenerator("m-");
    private final StringIdGenerator taskIdGenerator = new StringIdGenerator.DefaultIdGenerator("t-");

    @Override
    public String generateTaskId() {
        return messageIdGenerator.generateId();
    }

    @Override
    public String generateMessageId() {
        return taskIdGenerator.generateId();
    }

}

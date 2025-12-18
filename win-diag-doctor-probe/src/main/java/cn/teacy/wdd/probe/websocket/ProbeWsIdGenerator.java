package cn.teacy.wdd.probe.websocket;

import cn.teacy.wdd.common.interfaces.StringIdGenerator;
import cn.teacy.wdd.protocol.factory.WsMessageIdGenerator;

public class ProbeWsIdGenerator implements WsMessageIdGenerator {

    private final StringIdGenerator messageIdGenerator = new StringIdGenerator.DefaultIdGenerator("pm-");

    @Override
    public String generateTaskId() {
        // 探针不负责生成任务 ID，由服务器下发任务时指定
        return "";
    }

    @Override
    public String generateMessageId() {
        return messageIdGenerator.generateId();
    }

}

package cn.teacy.wdd.websocket.handler;

import cn.teacy.wdd.protocol.IWsProtocolHandler;
import cn.teacy.wdd.protocol.WsMessagePayload;
import cn.teacy.wdd.protocol.WsProtocolHandler;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@WsProtocolHandler
public class TaskStatusUpdateHandler implements IWsProtocolHandler {

    @Override
    public void handle(String taskId, WsMessagePayload payload) {
        log.info("TaskStatusUpdateHandler handle taskId={}", taskId);
        log.info("Payload: {}", payload);
    }

}

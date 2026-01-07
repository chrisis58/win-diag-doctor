package cn.teacy.wdd.websocket.handler;

import cn.teacy.wdd.protocol.IWsProtocolHandler;
import cn.teacy.wdd.protocol.WsMessagePayload;
import cn.teacy.wdd.protocol.WsProtocolHandler;
import cn.teacy.wdd.protocol.response.GetUserContextResponse;
import cn.teacy.wdd.service.IPendingTaskRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@WsProtocolHandler(protocols = {GetUserContextResponse.class})
public class GetUserContextResponseHandler implements IWsProtocolHandler {

    private final IPendingTaskRegistry<GetUserContextResponse> pendingTaskRegistry;

    @Override
    public void handle(String taskId, WsMessagePayload payload) {

        if (payload instanceof GetUserContextResponse response) {
            pendingTaskRegistry.complete(taskId, response);
        } else {
            log.warn("收到未知的 GetUserContextResponse 消息: taskId={}", taskId);
        }

    }

}

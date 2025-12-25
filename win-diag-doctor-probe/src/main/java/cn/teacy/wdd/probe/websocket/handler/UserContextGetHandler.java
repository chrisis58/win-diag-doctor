package cn.teacy.wdd.probe.websocket.handler;

import cn.teacy.wdd.common.entity.UserContext;
import cn.teacy.wdd.probe.component.ProbeContextProvider;
import cn.teacy.wdd.probe.websocket.IWsMessageSender;
import cn.teacy.wdd.protocol.IWsProtocolHandler;
import cn.teacy.wdd.protocol.WsMessagePayload;
import cn.teacy.wdd.protocol.WsProtocolHandler;
import cn.teacy.wdd.protocol.command.GetUserContext;
import cn.teacy.wdd.protocol.event.TaskStatusUpdate;
import cn.teacy.wdd.protocol.response.GetUserContextResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@WsProtocolHandler(protocols = {GetUserContext.class})
public class UserContextGetHandler implements IWsProtocolHandler {

    private final ProbeContextProvider probeContextProvider;
    private final IWsMessageSender messageSender;

    @Override
    public void handle(String taskId, WsMessagePayload payload) {

        log.info("received get user context request, taskId: {}", taskId);

        try {
            UserContext userContext = probeContextProvider.getUserContext();

            messageSender.send(new GetUserContextResponse(userContext), taskId);
        } catch (Exception e) {
            log.error("handle get user context failed, taskId: {}", taskId, e);
            messageSender.send(TaskStatusUpdate.fail(taskId, "Failed to get user context: " + e.getMessage()));
        }

    }
}

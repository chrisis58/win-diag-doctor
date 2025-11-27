package cn.teacy.wdd.websocket.handler;

import cn.teacy.wdd.protocol.IWsProtocolHandler;
import cn.teacy.wdd.protocol.WsMessagePayload;
import cn.teacy.wdd.protocol.WsProtocolHandler;
import cn.teacy.wdd.protocol.event.TaskStatusUpdate;
import cn.teacy.wdd.service.TaskRegistryRouter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@WsProtocolHandler(protocols = {TaskStatusUpdate.class})
public class TaskStatusUpdateHandler implements IWsProtocolHandler {

    private final TaskRegistryRouter taskRegistryRouter;

    @Override
    public void handle(String taskId, WsMessagePayload payload) {
        log.info("TaskStatusUpdateHandler handle taskId={}", taskId);

        TaskStatusUpdate statusUpdate = (TaskStatusUpdate) payload;

        switch (statusUpdate.getState()) {
            case RECEIVED:
                taskRegistryRouter.getTaskRegistry(taskId)
                        .ifPresent(registry ->
                            registry.ack(statusUpdate.getTaskId())
                        );
                break;
            case FAILED:
                taskRegistryRouter.getTaskRegistry(taskId)
                        .ifPresent(registry ->
                                registry.fail(statusUpdate.getTaskId(), new RuntimeException(statusUpdate.getMessage()))
                        );
                break;
        }

    }

}

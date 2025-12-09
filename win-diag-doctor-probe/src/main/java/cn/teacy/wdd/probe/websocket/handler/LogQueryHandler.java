package cn.teacy.wdd.probe.websocket.handler;

import cn.teacy.wdd.common.entity.TaskExecutionResult;
import cn.teacy.wdd.common.entity.WinEventLogEntry;
import cn.teacy.wdd.common.enums.ExecutionResultEndpoint;
import cn.teacy.wdd.probe.reader.IWinEventLogCleaner;
import cn.teacy.wdd.probe.reader.IWinEventLogReader;
import cn.teacy.wdd.probe.shipper.IProbeShipper;
import cn.teacy.wdd.probe.websocket.ProbeWsClient;
import cn.teacy.wdd.protocol.IWsProtocolHandler;
import cn.teacy.wdd.protocol.WsMessagePayload;
import cn.teacy.wdd.protocol.WsProtocolHandler;
import cn.teacy.wdd.protocol.command.LogQueryRequest;
import cn.teacy.wdd.protocol.event.TaskStatusUpdate;
import cn.teacy.wdd.protocol.response.LogQueryResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
@WsProtocolHandler(protocols = {LogQueryRequest.class})
public class LogQueryHandler implements IWsProtocolHandler {

    private final IProbeShipper shipper;
    private final IWinEventLogReader reader;
    private final IWinEventLogCleaner cleaner;

    private final ProbeWsClient wsClient;

    @Override
    public void handle(String taskId, WsMessagePayload payload) {

        log.info("received log query request, taskId: {}", taskId);
        wsClient.send(TaskStatusUpdate.ack(taskId));

        if (payload instanceof LogQueryRequest request) {

            try {
                LogQueryResponse logQueryResponse = reader.readEventLogs(request);

                List<WinEventLogEntry> handled = cleaner.handle(logQueryResponse.getEntries());
                logQueryResponse.setEntries(handled);

                boolean success = shipper.ship(ExecutionResultEndpoint.LogQuery, TaskExecutionResult.success(taskId, logQueryResponse));

                if (!success) {
                    wsClient.send(TaskStatusUpdate.fail(taskId, "Failed to ship log query result"));
                    log.error("failed to ship log query result, taskId: {}", taskId);
                }

            } catch (Exception e) {
                shipper.ship(ExecutionResultEndpoint.LogQuery, TaskExecutionResult.failure(taskId, e.getMessage()));
            }

        }

    }

}

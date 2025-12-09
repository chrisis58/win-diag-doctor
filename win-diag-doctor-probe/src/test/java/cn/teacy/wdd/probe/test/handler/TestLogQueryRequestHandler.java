package cn.teacy.wdd.probe.test.handler;

import cn.teacy.wdd.probe.reader.IWinEventLogReader;
import cn.teacy.wdd.protocol.IWsProtocolHandler;
import cn.teacy.wdd.protocol.WsMessagePayload;
import cn.teacy.wdd.protocol.WsProtocolHandler;
import cn.teacy.wdd.protocol.command.LogQueryRequest;
import cn.teacy.wdd.protocol.response.LogQueryResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;


@Slf4j
@RequiredArgsConstructor
@WsProtocolHandler(protocols = {LogQueryRequest.class})
public class TestLogQueryRequestHandler implements IWsProtocolHandler {

    private final IWinEventLogReader reader;

    @Override
    public void handle(String taskId, WsMessagePayload payload) {

        LogQueryRequest queryRequest = (LogQueryRequest) payload;

        LogQueryResponse queryResponse = reader.readEventLogs(queryRequest);

        log.info("处理日志查询请求，查询条件: {}, 读取到日志条目数: {}",
                queryRequest, queryResponse.getEntries().size()
        );

    }

}

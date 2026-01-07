package cn.teacy.wdd.agent.service;

import cn.teacy.wdd.protocol.command.LogQueryRequest;
import cn.teacy.wdd.protocol.response.LogQueryResponse;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.lang.Nullable;

public interface IEventLogQueryService {

    @Nullable
    LogQueryResponse queryEventLogs(LogQueryRequest request, ToolContext context);

}

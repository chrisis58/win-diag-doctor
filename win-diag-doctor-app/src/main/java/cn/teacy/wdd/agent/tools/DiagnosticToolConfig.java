package cn.teacy.wdd.agent.tools;

import cn.teacy.wdd.agent.prompt.PromptLoader;
import cn.teacy.wdd.agent.service.IEventLogQueryService;
import cn.teacy.wdd.agent.tools.annotations.DiagnosticTool;
import cn.teacy.wdd.protocol.command.LogQueryRequest;
import cn.teacy.wdd.protocol.response.LogQueryResponse;
import com.felipestanzani.jtoon.JToon;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


import static cn.teacy.wdd.agent.prompt.PromptIdentifier.QUERY_EVENT_LOG;
import static cn.teacy.wdd.constants.JToonEncodeOption.DEFAULT_OPTIONS;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class DiagnosticToolConfig {

    private final PromptLoader promptLoader;

    @Bean
    @DiagnosticTool
    public ToolCallback eventLogQueryTool(IEventLogQueryService eventLogQueryService) {
        return FunctionToolCallback
                .builder(QUERY_EVENT_LOG.getIdentifier(), (LogQueryRequest request, ToolContext context) -> {
                    log.info("Execute eventLogQueryTool with request: {}", request);

                    try {
                        LogQueryResponse queryResponse = eventLogQueryService.queryEventLogs(request, context);

                        log.info("Event log query response: {}", queryResponse);

                        if (queryResponse == null) {
                            return "EMPTY_RESULT";
                        }

                        return JToon.encode(queryResponse, DEFAULT_OPTIONS);

                    } catch (Exception e) {
                        return "日志查询失败: " + e.getMessage();
                    }
                })
                .description(promptLoader.loadPrompt(QUERY_EVENT_LOG))
                .inputType(LogQueryRequest.class)
                .build();
    }

}

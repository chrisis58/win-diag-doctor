package cn.teacy.wdd.agent.tools;

import cn.teacy.wdd.agent.prompt.PromptLoader;
import cn.teacy.wdd.agent.tools.annotations.DiagnosticTool;
import cn.teacy.wdd.protocol.command.LogQueryRequest;
import cn.teacy.wdd.protocol.response.LogQueryResponse;
import cn.teacy.wdd.service.LogQueryService;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.felipestanzani.jtoon.JToon;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;
import java.util.Optional;

import static cn.teacy.wdd.agent.prompt.PromptIdentifier.QUERY_EVENT_LOG;
import static cn.teacy.wdd.constants.JToonEncodeOption.DEFAULT_OPTIONS;
import static com.alibaba.cloud.ai.graph.agent.tools.ToolContextConstants.AGENT_CONFIG_CONTEXT_KEY;

@Configuration
@RequiredArgsConstructor
public class DiagnosticToolConfig {

    private final PromptLoader promptLoader;

    @Bean
    @DiagnosticTool
    public ToolCallback eventLogQueryTool(LogQueryService logQueryService) {
        return FunctionToolCallback
                .builder(QUERY_EVENT_LOG.getIdentifier(), (LogQueryRequest request, ToolContext context) -> {

                    Optional<Map<String, Object>> metadata = ((RunnableConfig) context.getContext().get(AGENT_CONFIG_CONTEXT_KEY)).metadata();

                    if (metadata.isEmpty()) {
                        return "ERROR: Probe ID missing in context.";
                    }

                    String probeId = metadata.get().get("probe_id").toString();

                    if (probeId == null || probeId.isBlank()) {
                        return "ERROR: Probe ID missing in context.";
                    }

                    try {
                        LogQueryResponse queryResponse = logQueryService.queryLog(probeId, request);

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

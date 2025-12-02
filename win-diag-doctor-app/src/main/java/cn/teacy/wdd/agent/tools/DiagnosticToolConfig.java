package cn.teacy.wdd.agent.tools;

import cn.teacy.wdd.agent.tools.annotations.DiagnosticTool;
import cn.teacy.wdd.common.entity.WinEventLogEntry;
import cn.teacy.wdd.protocol.command.LogQueryRequest;
import cn.teacy.wdd.service.LogQueryService;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import dev.toonformat.jtoon.JToon;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;

import static cn.teacy.wdd.agent.tools.ToonResultConverter.DEFAULT_OPTIONS;
import static com.alibaba.cloud.ai.graph.agent.tools.ToolContextConstants.AGENT_CONFIG_CONTEXT_KEY;

@Configuration
public class DiagnosticToolConfig {

    @Bean
    @DiagnosticTool
    public ToolCallback eventLogQueryTool(LogQueryService logQueryService) {
        return FunctionToolCallback
                .builder("eventLogQuery", new BiFunction<LogQueryRequest, ToolContext, String>() {
                    @Override
                    public String apply(LogQueryRequest request, ToolContext context) {


                        Optional<Map<String, Object>> metadata = ((RunnableConfig) context.getContext().get(AGENT_CONFIG_CONTEXT_KEY)).metadata();

                        if (metadata.isEmpty()) {
                            return "ERROR: Probe ID missing in context.";
                        }

                        String probeId = metadata.get().get("probe_id").toString();

                        if (probeId == null || probeId.isBlank()) {
                            return "ERROR: Probe ID missing in context.";
                        }

                        List<WinEventLogEntry> logs = logQueryService.queryLog(probeId, request);

                        return JToon.encode(logs, DEFAULT_OPTIONS);
                    }
                })
                .description("""
                    本工具用于查询 Windows 事件日志。接受一个 LogQueryRequest 对象作为输入，返回符合查询条件的 Windows 事件日志条目列表。
                    你可以使用此工具来获取特定日志名称、级别和时间范围内的事件日志，以帮助诊断和分析 Windows 系统中的问题。
                    """)
                .inputType(LogQueryRequest.class)
                .build();
    }

}

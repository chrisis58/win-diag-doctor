package cn.teacy.wdd.agent.tools;

import cn.teacy.wdd.agent.tools.annotations.DiagnosticTool;
import cn.teacy.wdd.protocol.command.LogQueryRequest;
import cn.teacy.wdd.protocol.response.LogQueryResponse;
import cn.teacy.wdd.service.LogQueryService;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.felipestanzani.jtoon.JToon;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;

import static cn.teacy.wdd.constants.JToonEncodeOption.DEFAULT_OPTIONS;
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

                        LogQueryResponse queryResponse = logQueryService.queryLog(probeId, request);

                        return JToon.encode(queryResponse, DEFAULT_OPTIONS);
                    }
                })
                .description("""
                    本工具用于查询 Windows 事件日志。接受一个 LogQueryRequest 对象作为输入，返回符合查询条件的 Windows 事件日志条目列表。
                    你可以使用此工具来获取特定日志名称、级别和时间范围内的事件日志，以帮助诊断和分析 Windows 系统中的问题。
                    
                    ## 核心解析规则
                    1. 元数据 (头部):
                       - hasMore: 若为 true，表示当前时间范围内的日志数量超过了返回限制 (被截断)。可以增大 'maxEvents' 参数再次查询，以获取遗漏的日志。
                       - userContext (权限):
                         - isAdmin: **读取 Security (安全) 日志必须为 true**。若为 false 且查询结果为空，务必建议用户以管理员身份运行。
                         - isReader: 若为 true，表示用户属于 Event Log Readers 组，可读取除 Security 外的系统日志。
                    2. 日志条目 (entries):
                       - Id: 故障检索核心关键字 (如 10016)。
                       - Message: 重点提取其中的错误码 (HResult) 及 DCOM 组件的 CLSID/APPID。
                    """)
                .inputType(LogQueryRequest.class)
                .build();
    }

}

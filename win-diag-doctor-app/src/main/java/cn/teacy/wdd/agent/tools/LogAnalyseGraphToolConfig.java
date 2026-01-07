package cn.teacy.wdd.agent.tools;

import cn.teacy.wdd.agent.graph.LogAnalyseGraphComposer;
import cn.teacy.wdd.agent.graph.service.LogAnalyseGraphService;
import cn.teacy.wdd.agent.prompt.PromptLoader;
import cn.teacy.wdd.agent.tools.annotations.LogAnalyseGraphTool;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;
import java.util.Optional;

import static cn.teacy.wdd.agent.prompt.PromptIdentifier.LOG_ANALYSE_EXPERT_DESCRIPTION;
import static com.alibaba.cloud.ai.graph.agent.tools.ToolContextConstants.AGENT_CONFIG_CONTEXT_KEY;

@Configuration
@RequiredArgsConstructor
public class LogAnalyseGraphToolConfig {

    private final PromptLoader promptLoader;

    @Bean
    @LogAnalyseGraphTool
    public ToolCallback logAnalyseExpert(LogAnalyseGraphService logAnalyseGraphService) {
        return FunctionToolCallback
                .builder(LOG_ANALYSE_EXPERT_DESCRIPTION.getIdentifier(), (String query, ToolContext context) -> {

                    try {
                        Optional<Map<String, Object>> metadata = ((RunnableConfig) context.getContext().get(AGENT_CONFIG_CONTEXT_KEY)).metadata();

                        if (metadata.isEmpty()) {
                            return null;
                        }

                        String probeId = metadata.get().get("probe_id").toString();

                        LogAnalyseGraphService.ExecResult result = logAnalyseGraphService.execute(probeId, query);

                        OverAllState state = result.result().state();
                        return (String) state.value(LogAnalyseGraphComposer.KEY_ANALYSE_REPORT).orElseGet(() ->
                            "Privilege Not Qualified: " + state.value(LogAnalyseGraphComposer.KEY_PRIVILEGE_QUALIFIED).orElse("")
                        );

                    } catch (Exception e) {
                        return "工作流执行失败: " + e.getMessage();
                    }
                })
                .description(promptLoader.loadPrompt(LOG_ANALYSE_EXPERT_DESCRIPTION))
                .inputType(String.class)
                .build();
    }

}

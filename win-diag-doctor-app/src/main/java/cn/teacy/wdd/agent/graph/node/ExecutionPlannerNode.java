package cn.teacy.wdd.agent.graph.node;

import cn.teacy.wdd.agent.graph.LogAnalyseGraphComposer;
import cn.teacy.wdd.agent.prompt.PromptIdentifier;
import cn.teacy.wdd.agent.prompt.PromptLoader;
import cn.teacy.wdd.agent.tools.annotations.DiagnosticTool;
import cn.teacy.wdd.agent.utils.ModelChatUtils;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.action.AsyncNodeActionWithConfig;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.felipestanzani.jtoon.JToon;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Component
public class ExecutionPlannerNode implements AsyncNodeActionWithConfig {

    private final ChatClient thinkChatClient;
    private final PromptLoader promptLoader;
    private final ObjectMapper objectMapper;
    private final List<ToolCallback> diagnosticTools;

    public ExecutionPlannerNode(
            @Qualifier("thinkChatClient") ChatClient thinkChatClient,
            PromptLoader promptLoader,
            ObjectMapper objectMapper,
            @DiagnosticTool List<ToolCallback> diagnosticTools
    ) {
        this.thinkChatClient = thinkChatClient;
        this.promptLoader = promptLoader;
        this.objectMapper = objectMapper;
        this.diagnosticTools = diagnosticTools;
    }

    private static final String KEY_QUERY = LogAnalyseGraphComposer.KEY_QUERY;
    private static final String KEY_EXECUTION_PLAN = LogAnalyseGraphComposer.KEY_EXECUTION_PLAN;
    private static final String KEY_EXECUTOR_INSTRUCTION = LogAnalyseGraphComposer.KEY_EXECUTOR_INSTRUCTION;

    public record PlannerResponse(
            @JsonProperty(required = true) String executionPlan,
            @JsonProperty(required = true) String executorInstruction
    ) {}

    private static final PlannerResponse EMPTY_RESPONSE = new PlannerResponse(null, null);

    @Override
    public CompletableFuture<Map<String, Object>> apply(OverAllState state, RunnableConfig config) {
        Optional<Object> query = state.value(KEY_QUERY);
        assert query.isPresent();
        String queryString = query.get().toString();

        BeanOutputConverter<PlannerResponse> converter = new BeanOutputConverter<>(PlannerResponse.class);

        ChatResponse response = thinkChatClient.prompt()
                .system(promptLoader.render(PromptIdentifier.LOG_ANALYSE_EXECUTION_PLANNER_SYS_PROMPT,
                        Map.of("format", converter.getFormat())
                ))
                .user(JToon.encode(Map.of(
                        "query", queryString,
                        "available_tools", diagnosticTools.stream().map(ToolCallback::getToolDefinition).map(ToolDefinition::description).toList()
                )))
                .call()
                .chatResponse();

        PlannerResponse plannerResponse = ModelChatUtils.extractOutput(response, EMPTY_RESPONSE, objectMapper);

        if (plannerResponse.executionPlan() == null || plannerResponse.executionPlan().isBlank()) {
            return CompletableFuture.completedFuture(
                    Map.of(KEY_EXECUTION_PLAN, "ERROR")
            );
        }

        return CompletableFuture.completedFuture(Map.of(
                KEY_EXECUTION_PLAN, plannerResponse.executionPlan(),
                KEY_EXECUTOR_INSTRUCTION, plannerResponse.executorInstruction()
        ));
    }
}

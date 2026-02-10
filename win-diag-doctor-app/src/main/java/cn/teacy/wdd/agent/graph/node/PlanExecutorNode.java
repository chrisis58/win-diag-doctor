package cn.teacy.wdd.agent.graph.node;

import cn.teacy.wdd.agent.graph.LogAnalyseGraphComposer;
import cn.teacy.wdd.agent.prompt.PromptIdentifier;
import cn.teacy.wdd.agent.prompt.PromptLoader;
import cn.teacy.wdd.agent.tools.annotations.DiagnosticTool;
import cn.teacy.wdd.agent.utils.ModelChatUtils;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.action.AsyncNodeActionWithConfig;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static com.alibaba.cloud.ai.graph.agent.tools.ToolContextConstants.AGENT_CONFIG_CONTEXT_KEY;

@Component
public class PlanExecutorNode implements AsyncNodeActionWithConfig {

    private final ChatClient flashChatClient;
    private final PromptLoader promptLoader;
    private final List<ToolCallback> diagnosticTools;

    public PlanExecutorNode(
            @Qualifier("flashChatClient") ChatClient flashChatClient,
            PromptLoader promptLoader,
            @DiagnosticTool List<ToolCallback> diagnosticTools
    ) {
        this.flashChatClient = flashChatClient;
        this.promptLoader = promptLoader;
        this.diagnosticTools = diagnosticTools;
    }

    private static final String KEY_EXECUTOR_INSTRUCTION = LogAnalyseGraphComposer.KEY_EXECUTOR_INSTRUCTION;
    private static final String KEY_EVENT_LOG_RESULT = LogAnalyseGraphComposer.KEY_EVENT_LOG_RESULT;

    record ExecutionRecord(
            @JacksonXmlProperty(localName = "action") String action,
            @JacksonXmlProperty(localName = "result") String result
    ) {}

    @Override
    public CompletableFuture<Map<String, Object>> apply(OverAllState state, RunnableConfig config) {
        Optional<Object> value = state.value(KEY_EXECUTOR_INSTRUCTION);
        assert value.isPresent();
        String executionInstruction = value.get().toString();

        ChatResponse response = flashChatClient.prompt()
                .system(promptLoader.loadPrompt(PromptIdentifier.LOG_ANALYSE_PLAN_EXECUTOR_SYS_PROMPT))
                .user(executionInstruction)
                .toolCallbacks(diagnosticTools)
                .toolContext(Map.of(AGENT_CONFIG_CONTEXT_KEY, config))
                .call()
                .chatResponse();

        String content = ModelChatUtils.extractContent(response, "ERROR");

        return CompletableFuture.completedFuture(Map.of(
                KEY_EVENT_LOG_RESULT, new ExecutionRecord(executionInstruction, content),
                KEY_EXECUTOR_INSTRUCTION, ""
        ));
    }
}

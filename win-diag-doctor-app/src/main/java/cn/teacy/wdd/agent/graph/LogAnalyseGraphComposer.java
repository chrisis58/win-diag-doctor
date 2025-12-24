package cn.teacy.wdd.agent.graph;

import cn.teacy.wdd.agent.common.GraphKey;
import cn.teacy.wdd.agent.common.GraphNode;
import cn.teacy.wdd.agent.node.InterruptableNodeAction;
import cn.teacy.wdd.agent.prompt.PromptIdentifier;
import cn.teacy.wdd.agent.prompt.PromptLoader;
import cn.teacy.wdd.agent.service.IUserContextProvider;
import cn.teacy.wdd.agent.tools.annotations.DiagnosticTool;
import cn.teacy.wdd.agent.utils.ModelChatUtils;
import cn.teacy.wdd.common.entity.UserContext;
import com.alibaba.cloud.ai.graph.*;
import com.alibaba.cloud.ai.graph.action.AsyncNodeActionWithConfig;
import com.alibaba.cloud.ai.graph.action.InterruptionMetadata;
import com.alibaba.cloud.ai.graph.checkpoint.config.SaverConfig;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import com.felipestanzani.jtoon.JToon;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Component
public class LogAnalyseGraphComposer extends AbstractGraphComposer {

    private final MemorySaver saver = new MemorySaver();

    @GraphKey
    public static final String KEY_QUERY = "query";

    @GraphKey
    private static final String KEY_PRIVILEGE_QUALIFIED = "privilege-qualified";

    @GraphKey
    public static final String KEY_EXECUTION_PLAN = "execution-plan";

    @GraphKey
    private static final String KEY_EVENT_LOG_ENTRIES = "event-log-entries";

    @GraphKey
    public static final String KEY_ANALYSE_REPORT = "analyse-report";

    @GraphNode("privilege-checker")
    private final AsyncNodeActionWithConfig privilegeCheckNode;

    @GraphNode("pre-check-result-handler")
    private final InterruptableNodeAction privilegeCheckResultHandleNode;

    @GraphNode("execution-planner")
    private final AsyncNodeActionWithConfig executionPlanner;

    record PrivilegeCheckerQuery(String query, UserContext userContext) {}

    /** 在此构造器中初始化各个节点 */
    public LogAnalyseGraphComposer(
            @Qualifier("flashChatClient") ChatClient flashChatClient,
            @Qualifier("thinkChatClient") ChatClient thinkChatClient,
            IUserContextProvider userContextProvider,
            PromptLoader promptLoader,
            @DiagnosticTool List<ToolCallback> diagnosticTools
    ) {

        this.privilegeCheckNode = (state, config) -> {

            Optional<Object> query = state.value(KEY_QUERY);
            if (query.isEmpty()) {
                return CompletableFuture.completedFuture(
                        Map.of(KEY_PRIVILEGE_QUALIFIED, "Cannot perform privilege check: query is missing.")
                );
            }

            String queryString = query.toString();

            UserContext userContext = userContextProvider.getUserContext(state, config);

            ChatResponse response = flashChatClient.prompt()
                    .system(promptLoader.loadPrompt(PromptIdentifier.PRIVILEGE_CHECKER_SYS_PROMPT))
                    .user(JToon.encode(new PrivilegeCheckerQuery(queryString, userContext)))
                    .call()
                    .chatResponse();

            String output = ModelChatUtils.extractContent(response, "权限服务响应异常");

            if (output.length() < 10 && output.toLowerCase().contains("true")) {
                output = "true";
            }

            return CompletableFuture.completedFuture(
                    Map.of(KEY_PRIVILEGE_QUALIFIED, output)
            );
        };

        this.privilegeCheckResultHandleNode = (nodeId, state, config) -> {

            Optional<Object> value = state.value(KEY_PRIVILEGE_QUALIFIED);

            if (value.isPresent() && String.valueOf(Boolean.TRUE).equals(value.get())) {
                // Privilege check passed
                return Optional.empty();
            }

            InterruptionMetadata interruption = InterruptionMetadata.builder(nodeId, state)
                    .addMetadata("message", value.isPresent()
                            ? value.get().toString()
                            : "Privilege check failed: insufficient permissions to perform log analysis.")
                    .addMetadata("node", nodeId)
                    .build();

            return Optional.of(interruption);
        };

        this.executionPlanner = (state, config) -> {

            Optional<Object> query = state.value(KEY_QUERY);
            assert query.isPresent();
            String queryString = query.get().toString();

            ChatResponse response = thinkChatClient.prompt()
                    .system(promptLoader.loadPrompt(PromptIdentifier.LOG_ANALYSE_EXECUTION_PLANNER_SYS_PROMPT))
                    .user(JToon.encode(Map.of(
                            "query", queryString,
                            "available_tools", diagnosticTools.stream().map(ToolCallback::getToolDefinition).map(ToolDefinition::description).toList()
                    )))
                    .call()
                    .chatResponse();

            String output = ModelChatUtils.extractContent(response, "计划生成服务响应异常");

            return CompletableFuture.completedFuture(
                    Map.of(KEY_EXECUTION_PLAN, output)
            );
        };

    }

    @Override
    protected void addEdges(StateGraph builder) throws GraphStateException {
        builder.addEdge(StateGraph.START, idOf(privilegeCheckNode));
        builder.addEdge(idOf(privilegeCheckNode), idOf(privilegeCheckResultHandleNode));
        builder.addEdge(idOf(privilegeCheckResultHandleNode), idOf(executionPlanner));
        builder.addEdge(idOf(executionPlanner), StateGraph.END);
    }

    @Override
    protected CompileConfig compileConfig() {
        return CompileConfig.builder()
                .saverConfig(SaverConfig.builder()
                        .register(saver)
                        .build())
                .build();
    }

    @Bean
    public CompiledGraph logAnalyseGraph() {
        return compose();
    }

}

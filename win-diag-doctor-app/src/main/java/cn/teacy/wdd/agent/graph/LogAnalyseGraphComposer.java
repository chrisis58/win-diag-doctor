package cn.teacy.wdd.agent.graph;

import cn.teacy.wdd.agent.common.GraphNode;
import cn.teacy.wdd.agent.common.GraphKeys;
import cn.teacy.wdd.agent.node.InterruptableNodeAction;
import cn.teacy.wdd.agent.prompt.PromptIdentifier;
import cn.teacy.wdd.agent.prompt.PromptLoader;
import cn.teacy.wdd.agent.service.IUserContextProvider;
import cn.teacy.wdd.agent.utils.ModelChatUtils;
import cn.teacy.wdd.common.entity.UserContext;
import com.alibaba.cloud.ai.graph.*;
import com.alibaba.cloud.ai.graph.action.AsyncNodeActionWithConfig;
import com.alibaba.cloud.ai.graph.action.InterruptionMetadata;
import com.alibaba.cloud.ai.graph.checkpoint.config.SaverConfig;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy;
import com.felipestanzani.jtoon.JToon;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.lang.NonNull;
import lombok.Getter;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Component
public class LogAnalyseGraphComposer extends AbstractGraphComposer {

    @Getter
    enum LogAnalyseGraphKeys implements GraphKeys {
        QUERY("query", new ReplaceStrategy()),
        PRIVILEGE_QUALIFIED("privilege-qualified", new ReplaceStrategy()),
        EXECUTION_PLAN("execution-plan", new ReplaceStrategy()),
        EVENT_LOG_ENTRIES("event-log-entries", new ReplaceStrategy()),
        ANALYSE_REPORT("analyse-report", new ReplaceStrategy())
        ;

        private final String key;
        private final KeyStrategy strategy;

        LogAnalyseGraphKeys(String key, KeyStrategy strategy) {
            this.key = key;
            this.strategy = strategy;
        }

    }

    private final MemorySaver saver = new MemorySaver();

    @GraphNode("privilege-check")
    private final AsyncNodeActionWithConfig privilegeCheckNode;

    @GraphNode("pre-check-result-handler")
    private final InterruptableNodeAction privilegeCheckResultHandleNode;

    record PrivilegeCheckerQuery(String query, UserContext userContext) {}

    @NonNull
    @Override
    protected Class<? extends GraphKeys> determineGraphKeysClass() {
        return LogAnalyseGraphKeys.class;
    }

    /** 在此构造器中初始化各个节点 */
    public LogAnalyseGraphComposer(
            @Qualifier("flashChatClient") ChatClient flashChatClient,
            @Qualifier("thinkChatClient") ChatClient thinkChatClient,
            IUserContextProvider userContextProvider,
            PromptLoader promptLoader
    ) {

        this.privilegeCheckNode = (state, config) -> {

            Optional<Object> query = state.value(LogAnalyseGraphKeys.QUERY.getKey());
            if (query.isEmpty()) {
                return CompletableFuture.completedFuture(
                        Map.of(LogAnalyseGraphKeys.PRIVILEGE_QUALIFIED.getKey(), "Cannot perform privilege check: query is missing.")
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
                    Map.of(LogAnalyseGraphKeys.PRIVILEGE_QUALIFIED.getKey(), output)
            );
        };

        this.privilegeCheckResultHandleNode = (nodeId, state, config) -> {

            Optional<Object> value = state.value(LogAnalyseGraphKeys.PRIVILEGE_QUALIFIED.getKey());

            if (value.isPresent() && Boolean.TRUE.equals(value.get())) {
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

    }

    @Override
    protected void addEdges(StateGraph builder) throws GraphStateException {
        builder.addEdge(StateGraph.START, idOf(privilegeCheckNode));
        builder.addEdge(idOf(privilegeCheckNode), idOf(privilegeCheckResultHandleNode));
        builder.addEdge(idOf(privilegeCheckResultHandleNode), StateGraph.END);
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

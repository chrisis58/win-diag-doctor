package cn.teacy.wdd.agent.graph;

import cn.teacy.wdd.agent.node.AsyncBaseNodeAction;
import cn.teacy.wdd.agent.common.GraphKeys;
import cn.teacy.wdd.agent.node.InterruptableNodeAction;
import cn.teacy.wdd.agent.utils.GraphUtils;
import com.alibaba.cloud.ai.graph.*;
import com.alibaba.cloud.ai.graph.action.InterruptionMetadata;
import com.alibaba.cloud.ai.graph.checkpoint.config.SaverConfig;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy;
import lombok.Getter;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Component
public class LogAnalyseGraphConstructor {

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

    private final AsyncBaseNodeAction privilegeCheckNode;
    private final InterruptableNodeAction privilegeCheckResultHandleNode;

    /** 在此构造器中初始化各个节点 */
    public LogAnalyseGraphConstructor(ChatClient chatClient) {

        this.privilegeCheckNode = new AsyncBaseNodeAction("privilege-check") {
            @Override
            public CompletableFuture<Map<String, Object>> apply(OverAllState state, RunnableConfig config) {
                // TODO: 调用聊天模型进行权限检查
                return CompletableFuture.completedFuture(
                        Map.of(LogAnalyseGraphKeys.PRIVILEGE_QUALIFIED.getKey(), Boolean.TRUE.toString())
                );
            }
        };


        this.privilegeCheckResultHandleNode = new InterruptableNodeAction("pre-check-result-handler", "权限检查结果判断") {
            @Override
            public Optional<InterruptionMetadata> interrupt(String nodeId, OverAllState state, RunnableConfig config) {

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
            }
        };


    }

    @Bean
    public CompiledGraph logAnalyseGraph() throws GraphStateException {

        StateGraph builder = new StateGraph(GraphUtils.buildKeyStrategyFactory(LogAnalyseGraphKeys.class))
                .addNode(privilegeCheckNode.getNodeId(), privilegeCheckNode)
                .addNode(privilegeCheckResultHandleNode.getNodeId(), privilegeCheckResultHandleNode);


        CompileConfig compileConfig = CompileConfig.builder()
                .saverConfig(SaverConfig.builder()
                        .register(saver)
                        .build())
                .build();

        return builder.compile(compileConfig);
    }

}

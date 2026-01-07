package cn.teacy.wdd.agent.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.action.AsyncNodeActionWithConfig;
import com.alibaba.cloud.ai.graph.action.InterruptableAction;
import com.alibaba.cloud.ai.graph.action.InterruptionMetadata;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@FunctionalInterface
public interface InterruptibleNodeAction extends InterruptableAction, AsyncNodeActionWithConfig {

    @Override
    Optional<InterruptionMetadata> interrupt(String nodeId, OverAllState state, RunnableConfig config);

    @Override
    default CompletableFuture<Map<String, Object>> apply(OverAllState state, RunnableConfig config) {
        // 返回空 Map，表示对 State 不做任何修改
        return CompletableFuture.completedFuture(Collections.emptyMap());
    }

}
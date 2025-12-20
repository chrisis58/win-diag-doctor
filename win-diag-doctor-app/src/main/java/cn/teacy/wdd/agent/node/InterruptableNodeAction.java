package cn.teacy.wdd.agent.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.action.InterruptableAction;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

public abstract class InterruptableNodeAction extends AsyncBaseNodeAction implements InterruptableAction {
    private final String message;

    public InterruptableNodeAction(String nodeId, String message) {
        super(nodeId);
        this.message = message;
    }

    /**
     * 仅返回预设的消息内容
     *
     */
    @Override
    public CompletableFuture<Map<String, Object>> apply(OverAllState state, RunnableConfig config) {
        return CompletableFuture.completedFuture(Map.of("messages", message));
    }

}

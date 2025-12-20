package cn.teacy.wdd.agent.node;

import com.alibaba.cloud.ai.graph.action.AsyncNodeActionWithConfig;
import lombok.Getter;

public abstract class AsyncBaseNodeAction implements AsyncNodeActionWithConfig {

    @Getter
    private final String nodeId;

    public AsyncBaseNodeAction(String nodeId) {
        this.nodeId = nodeId;
    }

}

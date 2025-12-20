package cn.teacy.wdd.agent.node;

import com.alibaba.cloud.ai.graph.action.NodeActionWithConfig;
import lombok.Getter;

public abstract class BaseNodeAction implements NodeActionWithConfig {

    @Getter
    private final String nodeId;

    public BaseNodeAction(String nodeId) {
        this.nodeId = nodeId;
    }

}

package cn.teacy.wdd.agent.service;

import cn.teacy.wdd.common.entity.UserContext;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;

public interface IUserContextProvider {

    UserContext getUserContext(OverAllState state, RunnableConfig config);

}

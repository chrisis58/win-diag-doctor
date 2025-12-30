package cn.teacy.wdd.config;

import cn.teacy.wdd.common.entity.UserContext;
import cn.teacy.wdd.protocol.response.GetUserContextResponse;
import cn.teacy.wdd.protocol.response.LogQueryResponse;
import cn.teacy.wdd.service.IPendingTaskRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TaskRegistryConfig {

    @Bean
    public IPendingTaskRegistry<LogQueryResponse> pendingTaskRegistry() {
        return new IPendingTaskRegistry.BasePendingTaskRegistry<>();
    }

    @Bean
    public IPendingTaskRegistry<GetUserContextResponse> getUserContextPendingTaskRegistry() {
        return new IPendingTaskRegistry.BasePendingTaskRegistry<>();
    }

}

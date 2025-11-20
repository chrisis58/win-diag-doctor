package cn.teacy.wdd.config;

import cn.teacy.wdd.common.entity.WinEventLogEntry;
import cn.teacy.wdd.service.IPendingTaskRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class TaskRegistryConfig {

    @Bean
    public IPendingTaskRegistry<List<WinEventLogEntry>> pendingTaskRegistry() {
        return new IPendingTaskRegistry.BasePendingTaskRegistry<>();
    }

}

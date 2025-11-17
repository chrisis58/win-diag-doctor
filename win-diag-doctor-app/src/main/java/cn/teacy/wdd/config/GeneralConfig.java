package cn.teacy.wdd.config;

import cn.teacy.wdd.common.interfaces.TaskIdGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.UUID;

@Configuration
public class GeneralConfig {

    @Bean
    public TaskIdGenerator taskIdGenerator() {
        return UUID.randomUUID()::toString;
    }

}

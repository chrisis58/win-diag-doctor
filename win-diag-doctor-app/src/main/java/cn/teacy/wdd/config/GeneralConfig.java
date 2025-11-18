package cn.teacy.wdd.config;

import cn.teacy.wdd.common.interfaces.TaskIdGenerator;
import cn.teacy.wdd.protocol.WsPayloadExtractor;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.UUID;

@Configuration
public class GeneralConfig {

    @Bean
    public TaskIdGenerator taskIdGenerator() {
        return UUID.randomUUID()::toString;
    }

    @Bean
    public WsPayloadExtractor wsPayloadExtractor(ObjectMapper objectMapper) {
        return new WsPayloadExtractor(objectMapper);
    }

}

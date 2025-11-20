package cn.teacy.wdd.config;

import cn.teacy.wdd.common.interfaces.TaskIdGenerator;
import cn.teacy.wdd.protocol.WsMessageMapper;
import cn.teacy.wdd.protocol.WsProtocolHandlerRegistry;
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
    public WsMessageMapper wsPayloadExtractor(ObjectMapper objectMapper) {
        return new WsMessageMapper(objectMapper);
    }

    @Bean
    public WsProtocolHandlerRegistry wsProtocolHandlerRegistry(WsMessageMapper wsMessageMapper) {
        return new WsProtocolHandlerRegistry(wsMessageMapper, "cn.teacy.wdd.websocket.handler");
    }

}

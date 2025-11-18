package cn.teacy.wdd.probe;

import cn.teacy.wdd.protocol.command.LogQueryRequest;
import cn.teacy.wdd.protocol.WsMessage;
import cn.teacy.wdd.protocol.WsPayloadExtractor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

public class TestPayloadExtractor {

    private AnnotationConfigApplicationContext context;

    private ObjectMapper objectMapper;

    private WsPayloadExtractor protocolExtractor;

    @BeforeEach
    void setUp() {
        context = new AnnotationConfigApplicationContext();
        context.register(TestConfig.class);
        context.refresh();

        objectMapper = context.getBean(ObjectMapper.class);
        protocolExtractor = context.getBean(WsPayloadExtractor.class);
    }

    @AfterEach
    void tearDown() {
        if (context != null) {
            context.close();
        }
    }

    @Test
    public void testExtract() throws JsonProcessingException {
        String message = "{\"mid\":\"m-00001\",\"identifier\":\"command:logs:query\",\"data\":{\"logName\":null,\"levels\":null,\"maxEvents\":0,\"startHoursAgo\":1,\"endHoursAgo\":null}}";
        WsMessage wsMessage = objectMapper.readValue(message, WsMessage.class);

        Object result = protocolExtractor.extract(wsMessage);
        System.out.println("Extracted object: " + result);

        LogQueryRequest extractedRequest = assertInstanceOf(LogQueryRequest.class, result);
        assertEquals(1, extractedRequest.getStartHoursAgo());
    }


    @Configuration
    static class TestConfig {

        @Bean
        public ObjectMapper objectMapper() {
            return new ObjectMapper();
        }

        @Bean
        public WsPayloadExtractor wsPayloadExtractor(ObjectMapper objectMapper) {
            return new WsPayloadExtractor(objectMapper);
        }

    }

}

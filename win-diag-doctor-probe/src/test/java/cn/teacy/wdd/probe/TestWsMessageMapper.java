package cn.teacy.wdd.probe;

import cn.teacy.wdd.protocol.WsMessagePayload;
import cn.teacy.wdd.protocol.command.LogQueryRequest;
import cn.teacy.wdd.protocol.WsMessage;
import cn.teacy.wdd.protocol.WsMessageMapper;
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

public class TestWsMessageMapper {

    private AnnotationConfigApplicationContext context;

    private WsMessageMapper wsMessageMapper;

    @BeforeEach
    void setUp() {
        context = new AnnotationConfigApplicationContext();
        context.register(TestConfig.class);
        context.refresh();

        wsMessageMapper = context.getBean(WsMessageMapper.class);
    }

    @AfterEach
    void tearDown() {
        if (context != null) {
            context.close();
        }
    }

    @Test
    public void testReadValue() throws JsonProcessingException {
        String message = "{\"mid\":\"m-00001\",\"identifier\":\"command:logs:query\",\"payload\":{\"logName\":null,\"levels\":null,\"maxEvents\":0,\"startHoursAgo\":1,\"endHoursAgo\":null}}";

        WsMessage<? extends WsMessagePayload> wsMessage = wsMessageMapper.read(message);
        System.out.println("Extracted object: " + wsMessage);

        LogQueryRequest extractedRequest = assertInstanceOf(LogQueryRequest.class, wsMessage.getPayload());
        assertEquals(1, extractedRequest.getStartHoursAgo());
    }


    @Configuration
    static class TestConfig {

        @Bean
        public ObjectMapper objectMapper() {
            return new ObjectMapper();
        }

        @Bean
        public WsMessageMapper wsPayloadExtractor(ObjectMapper objectMapper) {
            return new WsMessageMapper(objectMapper);
        }

    }

}

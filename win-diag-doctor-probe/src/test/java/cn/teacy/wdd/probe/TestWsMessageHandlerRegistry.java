package cn.teacy.wdd.probe;

import cn.teacy.wdd.common.enums.ExecuteOrder;
import cn.teacy.wdd.probe.component.PowerShellExecutor;
import cn.teacy.wdd.probe.component.ProbeContextProvider;
import cn.teacy.wdd.probe.reader.IWinEventLogReader;
import cn.teacy.wdd.probe.reader.PwshWinEventLogReader;
import cn.teacy.wdd.probe.test.handler.TestLogQueryRequestHandler;
import cn.teacy.wdd.protocol.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;
import java.util.Set;

public class TestWsMessageHandlerRegistry {

    private AnnotationConfigApplicationContext context;

    private WsMessageMapper wsMessageMapper;
    private WsProtocolHandlerRegistry wsProtocolHandlerRegistry;

    @BeforeEach
    void setUp() {
        context = new AnnotationConfigApplicationContext();
        context.register(TestConfig.class);
        context.refresh();

        wsMessageMapper = context.getBean(WsMessageMapper.class);
        wsProtocolHandlerRegistry = context.getBean(WsProtocolHandlerRegistry.class);
    }

    @AfterEach
    void tearDown() {
        if (context != null) {
            context.close();
        }
    }

    @Test
    void testHandleCommand() throws JsonProcessingException {

        String message = "{\"mid\":\"m-00001\",\"identifier\":\"command:logs:query\",\"payload\":{\"logName\":\"System\",\"levels\":[2,1],\"maxEvents\":3,\"startHoursAgo\":null,\"endHoursAgo\":null}}";

        WsMessage<? extends WsMessagePayload> wsMessage = wsMessageMapper.read(message);

        Map<ExecuteOrder, Set<IWsProtocolHandler>> handlers = wsProtocolHandlerRegistry.getHandlers(wsMessage.getPayload().getClass());

        for (ExecuteOrder executeOrder : ExecuteOrder.values()) {
            Set<IWsProtocolHandler> orderHandlers = handlers.get(executeOrder);
            if (orderHandlers != null) {
                for (IWsProtocolHandler handler : orderHandlers) {
                    handler.handle(wsMessage.getTaskId(), wsMessage.getPayload());
                }
            }
        }

    }

    @Configuration
    static class TestConfig {

        @Bean
        public ObjectMapper objectMapper() {
            return new ObjectMapper();
        }

        @Bean
        public PowerShellExecutor powerShellExecutor() {
            return new PowerShellExecutor();
        }

        @Bean
        public ProbeContextProvider probeContextProvider(PowerShellExecutor powerShellExecutor, ObjectMapper objectMapper) {
            return new ProbeContextProvider(powerShellExecutor, objectMapper);
        }

        @Bean
        public WsMessageMapper wsMessageMapper(ObjectMapper objectMapper) {
            return new WsMessageMapper(objectMapper);
        }

        @Bean
        public WsProtocolHandlerRegistry wsProtocolHandlerRegistry(WsMessageMapper wsMessageMapper) {
            return new WsProtocolHandlerRegistry(wsMessageMapper, "cn.teacy.wdd.probe.test.handler");
        }

        @Bean
        public IWinEventLogReader reader(ObjectMapper objectMapper, PowerShellExecutor powerShellExecutor, ProbeContextProvider probeContextProvider) {
            return new PwshWinEventLogReader(objectMapper, powerShellExecutor, probeContextProvider);
        }

        @Bean
        public TestLogQueryRequestHandler testLogQueryRequestHandler(IWinEventLogReader reader) {
            return new TestLogQueryRequestHandler(reader);
        }

    }

}

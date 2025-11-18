package cn.teacy.wdd;

import cn.teacy.wdd.protocol.WsMessage;
import cn.teacy.wdd.protocol.WsPayloadExtractor;
import cn.teacy.wdd.protocol.command.LogQueryRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

@SpringBootTest
public class TestPayloadConverter {

    @Autowired
    private WsPayloadExtractor protocolExtractor;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void testExtract() throws JsonProcessingException {

        String message = "{\"mid\":\"m-00001\",\"identifier\":\"command:logs:query\",\"data\":{\"logName\":null,\"levels\":null,\"maxEvents\":0,\"startHoursAgo\":1,\"endHoursAgo\":null}}";
        WsMessage wsMessage = objectMapper.readValue(message, WsMessage.class);

        Object result = protocolExtractor.extract(wsMessage);
        System.out.println("Extracted object: " + result);

        LogQueryRequest extractedRequest = assertInstanceOf(LogQueryRequest.class, result);
        assertEquals(1, extractedRequest.getStartHoursAgo());

    }


}

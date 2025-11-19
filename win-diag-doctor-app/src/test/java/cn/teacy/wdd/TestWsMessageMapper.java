package cn.teacy.wdd;

import cn.teacy.wdd.protocol.WsMessage;
import cn.teacy.wdd.protocol.WsMessageMapper;
import cn.teacy.wdd.protocol.WsMessagePayload;
import cn.teacy.wdd.protocol.command.LogQueryRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

@SpringBootTest
public class TestWsMessageMapper {

    @Autowired
    private WsMessageMapper wsMessageMapper;

    @Test
    void testReadValue() throws JsonProcessingException {
        String message = "{\"mid\":\"m-00001\",\"identifier\":\"command:logs:query\",\"payload\":{\"logName\":null,\"levels\":null,\"maxEvents\":0,\"startHoursAgo\":1,\"endHoursAgo\":null}}";

        WsMessage<? extends WsMessagePayload> wsMessage = wsMessageMapper.read(message);
        System.out.println("Extracted object: " + wsMessage);

        LogQueryRequest extractedRequest = assertInstanceOf(LogQueryRequest.class, wsMessage.getPayload());
        assertEquals(1, extractedRequest.getStartHoursAgo());
    }


}

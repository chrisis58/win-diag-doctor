package cn.teacy.wdd.websocket;

import cn.teacy.wdd.protocol.WsMessage;
import cn.teacy.wdd.protocol.WsMessagePayload;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.WebSocketSession;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class WsSessionManager {

    private final Map<String, WebSocketSession> probeSessions = new java.util.concurrent.ConcurrentHashMap<>();

    private final ObjectMapper objectMapper;

    public void register(String probeId, WebSocketSession session) {

        probeSessions.computeIfAbsent(probeId, (pid) -> {
            log.info("Register WebSocket session for probeId: {}, sessionId: {}", pid, session.getId());
            return session;
        });

    }

    public void close(String probeId) {
        WebSocketSession session = probeSessions.remove(probeId);
        if (session != null && session.isOpen()) {
            try {
                session.close();
                log.info("Closed WebSocket session for probeId: {}, sessionId: {}", probeId, session.getId());
            } catch (Exception e) {
                log.error("Error closing WebSocket session for probeId: {}, sessionId: {}", probeId, session.getId(), e);
            }
        }
    }

    public <P extends WsMessagePayload> void send(String probeId, WsMessage<P> wsMessage) {
        WebSocketSession session = probeSessions.get(probeId);
        if (session != null && session.isOpen()) {
            try {
                String messageText = objectMapper.writeValueAsString(wsMessage);
                session.sendMessage(new org.springframework.web.socket.TextMessage(messageText));
                log.info("Sent message to probeId: {}, sessionId: {}, message: {}", probeId, session.getId(), messageText);
            } catch (Exception e) {
                log.error("Error sending message to probeId: {}, sessionId: {}", probeId, session.getId(), e);
            }
        } else {
            log.warn("No open WebSocket session found for probeId: {}", probeId);
        }

    }

}

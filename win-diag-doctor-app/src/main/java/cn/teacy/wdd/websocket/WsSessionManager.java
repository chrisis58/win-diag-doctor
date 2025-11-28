package cn.teacy.wdd.websocket;

import cn.teacy.wdd.exception.ProbeNotConnectedException;
import cn.teacy.wdd.protocol.WsMessage;
import cn.teacy.wdd.protocol.WsMessagePayload;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.WebSocketSession;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class WsSessionManager {

    @Getter
    private final Map<String, ProbeSessionContext> probeContexts = new ConcurrentHashMap<>();

    private final ObjectMapper objectMapper;

    public void register(String probeId, WebSocketSession session) {
        String initialHostname = "Unknown-Host-" + probeId.substring(0, 8);
        register(probeId, initialHostname, session);
    }

    public void register(String probeId, String hostname, WebSocketSession session) {
        ProbeSessionContext oldContext = probeContexts.put(probeId, new ProbeSessionContext(session, hostname));

        if (oldContext != null) {
            log.warn("Probe {} reconnected. Replacing old session {} with {}",
                    probeId, oldContext.getSession().getId(), session.getId());
            closeSessionSafe(oldContext.getSession());
        } else {
            log.info("Registered new session for probeId: {}", probeId);
        }

    }

    public void close(String probeId) {
        ProbeSessionContext context = probeContexts.remove(probeId);
        if (context != null) {
            closeSessionSafe(context.getSession());
            log.info("Removed context for probeId: {}", probeId);
        }
    }

    public <P extends WsMessagePayload> void send(String probeId, WsMessage<P> wsMessage) {
        ProbeSessionContext context = probeContexts.get(probeId);

        if (context == null) {
            throw new ProbeNotConnectedException("Probe " + probeId + " not found.");
        }

        WebSocketSession session = context.getSession();
        if (session.isOpen()) {
            try {
                String messageText = objectMapper.writeValueAsString(wsMessage);
                session.sendMessage(new org.springframework.web.socket.TextMessage(messageText));
                log.info("Sent message to probeId: {}, sessionId: {}, message: {}", probeId, session.getId(), messageText);
            } catch (Exception e) {
                log.error("Error sending message to probeId: {}, sessionId: {}", probeId, session.getId(), e);
            }
        } else {
            close(probeId);
            throw new ProbeNotConnectedException("Probe " + probeId + " session is closed.");
        }
    }

    public Map<String, String> getProbeHostnames() {
        return probeContexts.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> Objects.requireNonNullElse(entry.getValue().getHostname(), "Unknown hostname.")
                ));
    }

    public void updateHostname(String probeId, String hostname) {
        ProbeSessionContext context = probeContexts.get(probeId);
        if (context != null) {
            context.hostname = hostname;
            log.info("Updated hostname for probe {}: {}", probeId, hostname);
        }
    }

    private void closeSessionSafe(WebSocketSession session) {
        try {
            if (session != null && session.isOpen()) {
                session.close();
            }
        } catch (Exception e) {
            // ignore
        }
    }

    @Getter
    @RequiredArgsConstructor
    @AllArgsConstructor
    public static class ProbeSessionContext {
        private final WebSocketSession session;
        private volatile String hostname;
    }

}

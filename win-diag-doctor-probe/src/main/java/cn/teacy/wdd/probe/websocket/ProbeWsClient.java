package cn.teacy.wdd.probe.websocket;

import cn.teacy.wdd.common.enums.ExecuteOrder;
import cn.teacy.wdd.probe.properties.IProbeProperties;
import cn.teacy.wdd.probe.utils.HostnameUtil;
import cn.teacy.wdd.protocol.*;
import cn.teacy.wdd.protocol.event.ProbeHeartbeat;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static cn.teacy.wdd.common.constants.ServiceConstants.WS_PROBE_ENDPOINT;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProbeWsClient implements WebSocket.Listener, SmartLifecycle, IWsMessageSender {

    private static final int MAX_RETRY_ATTEMPTS = 5;

    private final IProbeProperties properties;
    private final HttpClient httpClient;
    private final WsMessageMapper wsMessageMapper;
    private final WsProtocolHandlerRegistry registry;
    private final ObjectMapper objectMapper;

    private WebSocket webSocket;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private final AtomicInteger retryCount = new AtomicInteger(0);
    private ScheduledFuture<?> heartbeatTask;

    private volatile boolean running;

    @Override
    public boolean isRunning() {
        return this.running;
    }

    @Override
    public void stop() {
        log.info("Stopping ProbeWsClient");
        stopHeartbeat();

        if (!scheduler.isShutdown()) {
            scheduler.shutdownNow();
        }

        if (webSocket != null && !webSocket.isOutputClosed()) {
            webSocket.sendClose(WebSocket.NORMAL_CLOSURE, "Probe shutting down");
        }
        this.running = false;
    }

    @Override
    public void start() {
        log.info("Starting ProbeWsClient");
        this.connect();
        this.running = true;
    }

    public void connect() {
        try {
            String wsUrl = String.format("%s%s?probeId=%s&hostname=%s",
                    properties.getWsServerHost(),
                    WS_PROBE_ENDPOINT,
                    properties.getProbeId(),
                    HostnameUtil.getHostname()
            );
            log.info("Connecting to WebSocket: {}", wsUrl);

            // 建立连接并携带认证头
            httpClient.newWebSocketBuilder()
                    .header("Authorization", "Bearer " + properties.getProbeSecret())
                    .connectTimeout(Duration.ofSeconds(10))
                    .buildAsync(URI.create(wsUrl), this)
                    .thenAccept(ws -> {
                        this.webSocket = ws;
                        this.retryCount.set(0);
                    })
                    .exceptionally(ex -> {
                        log.error("Fail to acquire WebSocket connect: {}", ex.getMessage());
                        scheduleReconnect();
                        return null;
                    });

        } catch (Exception e) {
            log.error("Fail to initialize WebSocket", e);
            scheduleReconnect();
        }
    }

    /**
     * 发送文本消息 (供 Handler 使用)
     */
    public void send(String text) {
        if (webSocket != null && !webSocket.isOutputClosed()) {
            webSocket.sendText(text, true);
        } else {
            log.warn("Failed to send message: WebSocket not connected");
        }
    }

    public void send(WsMessagePayload payload) {
        try {
            WsMessage<?> wsMessage = new WsMessage<>(payload);

            this.send(objectMapper.writeValueAsString(wsMessage));
            log.debug("WsMessage sent, mid: {}", wsMessage.getMid());
        } catch (JsonProcessingException e) {
            log.error("Fail to serialize heartbeat: ", e);
        } catch (Exception e) {
            log.error("Error occur during sending heartbeat", e);
        }
    }

    @Override
    public void onOpen(WebSocket webSocket) {
        WebSocket.Listener.super.onOpen(webSocket);
        this.startHeartbeat();
    }

    @Override
    public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {

        String msg = data.toString();
        handleIncomingMessage(msg);

        return WebSocket.Listener.super.onText(webSocket, data, last);
    }

    @Override
    public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
        log.warn("WebSocket Connection closed: code={}, reason={}", statusCode, reason);
        stopHeartbeat();

        this.webSocket = null;
        scheduleReconnect();
        return WebSocket.Listener.super.onClose(webSocket, statusCode, reason);
    }

    @Override
    public void onError(WebSocket webSocket, Throwable error) {
        log.error("WebSocket encounter error", error);
        stopHeartbeat();

        this.webSocket = null;
        scheduleReconnect();
    }

    private void handleIncomingMessage(String msg) {
        try {
            WsMessage<? extends WsMessagePayload> wsMessage = wsMessageMapper.read(msg);

            Class<?> payloadClass = wsMessage.getPayload().getClass();
            Map<ExecuteOrder, Set<IWsProtocolHandler>> handlers = registry.getHandlers(payloadClass);

            if (handlers != null) {
                for (ExecuteOrder order : ExecuteOrder.values()) {
                    Set<IWsProtocolHandler> orderHandlers = handlers.get(order);
                    if (orderHandlers != null) {
                        for (IWsProtocolHandler handler : orderHandlers) {
                            try {
                                handler.handle(wsMessage.getTaskId(), wsMessage.getPayload());
                            } catch (Exception e) {
                                log.error("Handler fail: {}", handler.getClass().getSimpleName(), e);
                            }
                        }
                    }
                }
            }

        } catch (JsonProcessingException e) {
            log.error("Fail to deserialize message: {}", msg, e);
        }
    }

    private void scheduleReconnect() {
        int currentAttempt = retryCount.incrementAndGet();
        if (currentAttempt > MAX_RETRY_ATTEMPTS) {
            log.error("已达到最大重试次数 ({})，停止自动重连。请检查网络配置或重启探针。", MAX_RETRY_ATTEMPTS);
            log.info("当前配置信息: {}", properties.toString());
            System.exit(1);
            return;
        }

        // 按照指数退避算法计算延迟时间（秒）
        // 如果后续重试次数增加，可以考虑设置一个上限
        long delay = 5 * (long) Math.pow(2, currentAttempt - 1);

        log.info("Retry connecting after {}s...", delay);
        scheduler.schedule(this::connect, delay, TimeUnit.SECONDS);
    }

    private void startHeartbeat() {
        stopHeartbeat();

        this.heartbeatTask = scheduler.scheduleAtFixedRate(() -> {
            if (this.webSocket == null || this.webSocket.isOutputClosed()) {
                log.warn("Skipping heartbeat: WebSocket not connected");
                return;
            }

            try {
                WsMessage<ProbeHeartbeat> heartbeatMsg = new WsMessage<>(new ProbeHeartbeat());

                this.send(objectMapper.writeValueAsString(heartbeatMsg));
                log.debug("Heartbeat sent, mid: {}", heartbeatMsg.getMid());
            } catch (JsonProcessingException e) {
                log.error("Fail to serialize heartbeat: ", e);
            } catch (Exception e) {
                log.error("Error occur during sending heartbeat", e);
            }
        }, 10, 30, TimeUnit.SECONDS);
    }

    private void stopHeartbeat() {
        if (heartbeatTask != null && !heartbeatTask.isCancelled()) {
            heartbeatTask.cancel(true);
            log.info("Stop heartbeat task");
        }
        heartbeatTask = null;
    }
}

package cn.teacy.wdd.config.handler;

import cn.teacy.wdd.common.enums.ExecuteOrder;
import cn.teacy.wdd.protocol.*;
import cn.teacy.wdd.websocket.WsSessionManager;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProbeWsHandler extends TextWebSocketHandler {

    private final ScheduledThreadPoolExecutor protocolHandlerPool = new ScheduledThreadPoolExecutor(16, new ThreadFactory() {
        private final AtomicInteger threadNumber = new AtomicInteger(1);

        @Override
        public Thread newThread(@NonNull Runnable r) {
            Thread t = new Thread(r, "WsProtocolHandlerPool-" + threadNumber.getAndIncrement());

            if (t.isDaemon()) {
                t.setDaemon(false);
            }
            if (t.getPriority() != Thread.NORM_PRIORITY) {
                t.setPriority(Thread.NORM_PRIORITY);
            }
            return t;
        }
    });

    private final WsMessageMapper wsMessageMapper;
    private final WsSessionManager sessionManager;
    private final WsProtocolHandlerRegistry registry;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        String probeId = (String) session.getAttributes().get("probeId");

        if (probeId != null) {
            sessionManager.register(probeId, session);
        } else {
            try { session.close(); } catch (Exception ignored) {}
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, @NonNull CloseStatus status) {
        String probeId = (String) session.getAttributes().get("probeId");
        if (probeId != null) {
            sessionManager.close(probeId);
        }
    }

    @Override
    protected void handleTextMessage(@NonNull WebSocketSession session, @NonNull TextMessage message) {
        String msg = message.getPayload();
        try {
            WsMessage<? extends WsMessagePayload> wsMessage = wsMessageMapper.read(msg);

            Map<ExecuteOrder, Set<IWsProtocolHandler>> handlers = registry.getHandlers(wsMessage.getPayload().getClass());
            CompletableFuture<Void> executionChain = CompletableFuture.completedFuture(null);

            for (ExecuteOrder order : ExecuteOrder.values()) {
                Set<IWsProtocolHandler> orderHandlers = handlers.get(order);
                if (orderHandlers == null || orderHandlers.isEmpty()) {
                    continue;
                }

                executionChain = executionChain.thenCompose(unused -> {
                    List<CompletableFuture<Void>> currentOrderFutures = new ArrayList<>();

                    for (IWsProtocolHandler handler : orderHandlers) {
                        CompletableFuture<Void> cf = CompletableFuture.runAsync(() -> {
                            try {
                                handler.handle(wsMessage.getTaskId(), wsMessage.getPayload());
                            } catch (Exception e) {
                                // 当前 Handler 处理失败，抛出异常以中断后续的执行链
                                // 注意：不会中断同级的其他 Handler 执行
                                throw new RuntimeException("Handler failed: " + handler.getClass().getName(), e);
                            }
                        }, protocolHandlerPool);
                        currentOrderFutures.add(cf);
                    }

                    return CompletableFuture.allOf(currentOrderFutures.toArray(new CompletableFuture[0]));
                });

                // 统一处理最终结果或异常
                executionChain.whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("WebSocket message handling failed for msg: {}", msg, ex);
                    } else {
                        log.debug("Message processed successfully");
                    }
                });
            }

        } catch (JsonProcessingException e) {
            log.error("Failed to parse WebSocket message: {}", msg, e);
        }
    }

}

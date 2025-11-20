package cn.teacy.wdd.config;

import cn.teacy.wdd.config.handler.ProbeWsHandler;
import cn.teacy.wdd.config.interceptor.ProbeHandshakeInterceptor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

import static cn.teacy.wdd.common.constants.ServiceConstants.WS_PROBE_ENDPOINT;

@Slf4j
@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WsConfig implements WebSocketConfigurer {

    private final ProbeWsHandler probeWsHandler;
    private final ProbeHandshakeInterceptor probeHandshakeInterceptor;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(probeWsHandler, WS_PROBE_ENDPOINT)
                .addInterceptors(probeHandshakeInterceptor)
                .setAllowedOrigins("*");
    }

}

package cn.teacy.wdd.config.interceptor;

import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;
import java.util.Objects;

@Component
public class ProbeHandshakeInterceptor implements HandshakeInterceptor {

    @Value("${wdd.probe.connect-key}")
    private String probeConnectKey;

    @Override
    public boolean beforeHandshake(@NonNull ServerHttpRequest request, @NonNull ServerHttpResponse response, @NonNull WebSocketHandler wsHandler, @NonNull Map<String, Object> attributes) throws Exception {

        String authHeader = request.getHeaders().getFirst("Authorization");
        MultiValueMap<String, String> queryParams = UriComponentsBuilder.fromUri(request.getURI())
                .build()
                .getQueryParams();
        String probeId = Objects.requireNonNull(queryParams.getFirst("probeId"));

        if (Objects.isNull(authHeader) || !authHeader.equals("Bearer " + DigestUtils.md5Hex(probeConnectKey + probeId))) {
            return false;
        }

        String hostname = queryParams.getFirst("hostname");

        attributes.put("probeId", probeId);
        attributes.put("hostname", hostname != null ? hostname : "Unknown-Host-" + probeId.substring(0, 8));
        return true;

    }

    @Override
    public void afterHandshake(@NonNull ServerHttpRequest request, @NonNull ServerHttpResponse response, @NonNull WebSocketHandler wsHandler, Exception exception) {

    }

}

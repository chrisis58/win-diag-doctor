package cn.teacy.wdd.probe.shipper;

import cn.teacy.wdd.probe.properties.IProbeProperties;
import cn.teacy.wdd.protocol.WsMessageContext;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Slf4j
@Component
@RequiredArgsConstructor
public class HttpProbeShipper implements IProbeShipper {

    private static final String ANALYZE_ENDPOINT = "/api/analyze/logs/resolve";

    private final IProbeProperties properties;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    @Override
    public boolean ship(String taskId, WsMessageContext queryContext) {

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(properties.getServerHost() + ANALYZE_ENDPOINT))
                    .timeout(Duration.ofMinutes(2))
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(queryContext)))
                    .build();

            HttpResponse<String> send = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (send.statusCode() == 200) {
                log.info("分析结果: {}", send.body());
            } else {
                log.error("分析请求失败，状态码: {}, 响应体: {}", send.statusCode(), send.body());
            }

        } catch (JsonProcessingException e) {
            log.error("无法序列化日志条目: {}", e.getMessage());
            return false;
        } catch (IOException | InterruptedException e) {
            log.error("发送 HTTP 请求时出错", e);
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            return false;
        }

        return true;
    }

}

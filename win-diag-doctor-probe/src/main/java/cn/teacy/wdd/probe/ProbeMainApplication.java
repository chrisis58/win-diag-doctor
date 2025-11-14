package cn.teacy.wdd.probe;

import cn.teacy.wdd.common.constants.LogLevel;
import cn.teacy.wdd.common.constants.LogNames;
import cn.teacy.wdd.common.dto.LogQueryContext;
import cn.teacy.wdd.common.dto.LogQueryRequest;
import cn.teacy.wdd.common.dto.WinEventLogEntry;
import cn.teacy.wdd.probe.reader.IWinEventLogReader;
import cn.teacy.wdd.probe.reader.PwshWinEventLogReader;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;

@Slf4j
public class ProbeMainApplication {

    private static final String SERVER_ADDR = "http://localhost:8093";

    private static final String ANALYZE_ENDPOINT = "/api/analyze/logs/resolve";

    public static void main(String[] args) {

        ObjectMapper objectMapper = new ObjectMapper();
        HttpClient httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(10))
                .build();

        IWinEventLogReader reader = new PwshWinEventLogReader(objectMapper);

        LogQueryRequest queryRequest = LogQueryRequest.builder()
                .logName(LogNames.SYSTEM)
                .levels(List.of(LogLevel.CRITICAL, LogLevel.ERROR))
                .maxEvents(10)
                .build();

        List<WinEventLogEntry> logEntries = reader.readEventLogs(queryRequest);

        LogQueryContext logQueryContext = new LogQueryContext(queryRequest, logEntries);

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(SERVER_ADDR + ANALYZE_ENDPOINT))
                    .timeout(Duration.ofMinutes(2))
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(logQueryContext)))
                    .build();

            HttpResponse<String> send = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (send.statusCode() == 200) {
                log.info("分析结果: {}", send.body());
            } else {
                log.error("分析请求失败，状态码: {}, 响应体: {}", send.statusCode(), send.body());
            }

        } catch (JsonProcessingException e) {
            log.error("无法序列化日志条目: {}", e.getMessage());
        } catch (IOException | InterruptedException e) {
            log.error("发送 HTTP 请求时出错", e);
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
        }

    }

}

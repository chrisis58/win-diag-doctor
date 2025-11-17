package cn.teacy.wdd.probe;

import cn.teacy.wdd.common.constants.LogLevel;
import cn.teacy.wdd.common.constants.LogNames;
import cn.teacy.wdd.common.dto.LogQueryContext;
import cn.teacy.wdd.common.dto.LogQueryRequest;
import cn.teacy.wdd.common.dto.WinEventLogEntry;
import cn.teacy.wdd.probe.config.IProbeProperties;
import cn.teacy.wdd.probe.config.ProbePropertiesFromEnv;
import cn.teacy.wdd.probe.reader.IWinEventLogReader;
import cn.teacy.wdd.probe.reader.PwshWinEventLogReader;
import cn.teacy.wdd.probe.shipper.HttpProbeShipper;
import cn.teacy.wdd.probe.shipper.IProbeShipper;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;

@Slf4j
public class ProbeMainApplication {

    public static void main(String[] args) {

        ObjectMapper objectMapper = new ObjectMapper();
        HttpClient httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(10))
                .build();

        IProbeProperties properties = new ProbePropertiesFromEnv();
        IWinEventLogReader reader = new PwshWinEventLogReader(objectMapper);
        IProbeShipper shipper = new HttpProbeShipper(properties, httpClient, objectMapper);

        LogQueryRequest queryRequest = LogQueryRequest.builder()
                .logName(LogNames.SYSTEM)
                .levels(List.of(LogLevel.CRITICAL, LogLevel.ERROR))
                .maxEvents(10)
                .build();

        List<WinEventLogEntry> logEntries = reader.readEventLogs(queryRequest);
        log.debug("读取到的日志条目: {}", logEntries);

        LogQueryContext logQueryContext = new LogQueryContext(queryRequest, logEntries);

        boolean ret = shipper.ship("test-task-id", logQueryContext);
        log.info("日志发送结果: {}", ret ? "成功" : "失败");

    }

}

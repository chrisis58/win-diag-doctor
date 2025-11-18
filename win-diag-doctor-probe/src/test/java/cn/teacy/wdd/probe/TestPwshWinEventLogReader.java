package cn.teacy.wdd.probe;

import cn.teacy.wdd.common.constants.LogLevel;
import cn.teacy.wdd.common.constants.LogNames;
import cn.teacy.wdd.common.entity.WinEventLogEntry;
import cn.teacy.wdd.probe.reader.IWinEventLogReader;
import cn.teacy.wdd.probe.reader.PwshWinEventLogReader;
import cn.teacy.wdd.protocol.command.LogQueryRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

public class TestPwshWinEventLogReader {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private IWinEventLogReader logReader;

    @BeforeEach
    void setUp() {
        logReader = new PwshWinEventLogReader(objectMapper);
    }

    @Test
    @EnabledOnOs(OS.WINDOWS)
    void testReadEventLogs_ShouldReturnValidData() {

        LogQueryRequest request = new LogQueryRequest();
        request.setLogName(LogNames.SYSTEM);
        request.setLevels(List.of(LogLevel.ERROR, LogLevel.CRITICAL));
        request.setMaxEvents(5);

        List<WinEventLogEntry> logs = logReader.readEventLogs(request);

        assertNotNull(logs, "日志列表不应为 null");

        assertTrue(logs.size() <= 5, "返回的日志数量应不多于 " + request.getMaxEvents());

        if (!logs.isEmpty()) {
            WinEventLogEntry firstLog = logs.get(0);
            assertNotNull(firstLog.getMessage(), "日志消息不应为 null");
            assertNotNull(firstLog.getProviderName(), "ProviderName 不应为 null");
            assertTrue(
                    firstLog.getLevel().equals("Error") || firstLog.getLevel().equals("Critical"),
                    "日志级别应为 '错误' 或 '严重'"
            );
        }
    }

    @Test
    @EnabledOnOs(OS.WINDOWS)
    void testReadEventLogs_AllowNullFilter() {

        LogQueryRequest queryRequest = LogQueryRequest.builder()
                .logName(LogNames.SYSTEM)
                .maxEvents(3)
                .levels(null)
                .build();

        List<WinEventLogEntry> logs = logReader.readEventLogs(queryRequest);

        assertNotNull(logs, "日志列表不应为 null");

        assertTrue(logs.size() <= 3, "返回的日志数量应不多于 " + queryRequest.getMaxEvents());

        if (!logs.isEmpty()) {
            WinEventLogEntry firstLog = logs.get(0);
            assertNotNull(firstLog.getMessage(), "日志消息不应为 null");
            assertNotNull(firstLog.getProviderName(), "ProviderName 不应为 null");
        }
    }

    @Test
    @EnabledOnOs(OS.WINDOWS)
    void testReadEventLogs_EmptyResult() {
        LogQueryRequest queryRequest = LogQueryRequest.builder()
                .logName(LogNames.APPLICATION)
                .levels(List.of(LogLevel.CRITICAL))
                .endHoursAgo(300)
                .maxEvents(3)
                .build();

        List<WinEventLogEntry> logs = logReader.readEventLogs(queryRequest);

        assertNotNull(logs, "日志列表不应为 null");

        assertEquals(0, logs.size(), "预期没有日志返回，但实际有 " + logs.size() + " 条日志");
    }

    @Test
    @EnabledOnOs(OS.WINDOWS)
    void testReadEventLogs_InvalidTimeRange() {
        LogQueryRequest queryRequest = LogQueryRequest.builder()
                .logName(LogNames.SYSTEM)
                .maxEvents(3)
                .startHoursAgo(1) // from 1 hour ago
                .endHoursAgo(3) // to 3 hours ago
                .build();

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            logReader.readEventLogs(queryRequest);
        });

        assertNotNull(exception, "预期抛出 IllegalArgumentException，但没有抛出");

    }

    @Test
    @EnabledOnOs(OS.WINDOWS)
    void testReadEventLogs_filteredByTimeShift() {

        LogQueryRequest queryRequest = LogQueryRequest.builder()
                .logName(LogNames.SYSTEM)
                .maxEvents(3)
                .startHoursAgo(3) // from 3 hours ago
                .endHoursAgo(1) // to 1 hour ago
                .build();

        List<WinEventLogEntry> logs = logReader.readEventLogs(queryRequest);

        assertNotNull(logs, "日志列表不应为 null");

        assertTrue(logs.size() <= 3, "返回的日志数量应不多于 " + queryRequest.getMaxEvents());

        if (!logs.isEmpty()) {
            WinEventLogEntry firstLog = logs.get(0);
            assertNotNull(firstLog.getMessage(), "日志消息不应为 null");
            assertNotNull(firstLog.getProviderName(), "ProviderName 不应为 null");
        }

        Instant testRunTime = Instant.now();

        for (WinEventLogEntry log : logs) {
            assertNotNull(log.getMessage(), "日志消息不应为 null");
            assertNotNull(log.getProviderName(), "ProviderName 不应为 null");

            Instant logTime = parseMicrosoftTimestamp(log.getTimeCreated());

            Instant boundaryStart = testRunTime.minus(queryRequest.getStartHoursAgo(), ChronoUnit.HOURS)
                    .minusSeconds(5); // 允许5秒钟的时间偏差
            Instant boundaryEnd = testRunTime.minus(queryRequest.getEndHoursAgo(), ChronoUnit.HOURS)
                    .plusSeconds(5);

            assertFalse(logTime.isBefore(boundaryStart), "日志时间 " + logTime + " 早于起始边界 " + boundaryStart);

            assertFalse(logTime.isAfter(boundaryEnd), "日志时间 " + logTime + " 晚于结束边界 " + boundaryEnd);
        }

    }

    private Instant parseMicrosoftTimestamp(String msTimestamp) {

        Pattern pattern = Pattern.compile("/Date\\((\\d+)\\)/");
        Matcher matcher = pattern.matcher(msTimestamp);

        if (matcher.find()) {
            long epochMilli = Long.parseLong(matcher.group(1));

            return Instant.ofEpochMilli(epochMilli);
        } else {
            throw new IllegalArgumentException("无法解析的时间戳格式: " + msTimestamp);
        }
    }

}

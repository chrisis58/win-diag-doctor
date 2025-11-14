package cn.teacy.wdd.probe;

import cn.teacy.wdd.common.constants.LogLevel;
import cn.teacy.wdd.common.constants.LogNames;
import cn.teacy.wdd.common.dto.LogQueryRequest;
import cn.teacy.wdd.common.dto.WinEventLogEntry;
import cn.teacy.wdd.probe.reader.IWinEventLogReader;
import cn.teacy.wdd.probe.reader.PwshWinEventLogReader;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

        System.out.println(logs);

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

}

package cn.teacy.wdd.probe.reader;

import cn.teacy.wdd.common.enums.LogLevel;
import cn.teacy.wdd.common.entity.WinEventLogEntry;
import cn.teacy.wdd.common.utils.ReUtils;
import cn.teacy.wdd.protocol.command.LogQueryRequest;
import cn.teacy.wdd.protocol.response.LogQueryResponse;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 使用 PowerShell 读取 Windows 事件日志的实现
 */
@Slf4j
@Component
public class PwshWinEventLogReader implements IWinEventLogReader {

    private static final int TIMEOUT_SECONDS = 30;

    private final ObjectMapper objectMapper;

    public PwshWinEventLogReader(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public LogQueryResponse readEventLogs(LogQueryRequest queryRequest) {

        String powerShellCommand = EventViewerFilterCommandBuilder.builder()
                .logName(queryRequest.getLogName())
                .levels(queryRequest.getLevels())
                .maxEvents(queryRequest.getMaxEvents())
                .startHoursAgo(queryRequest.getStartHoursAgo())
                .endHoursAgo(queryRequest.getEndHoursAgo())
                .build();

        String[] command = {
                "cmd.exe",
                "/C",
                // "chcp 65001 >NUL" 切换代码页并抑制 "Active code page: 65001" 这条消息
                // "&&" 确保只有 chcp 成功后才执行 powershell
                // -NoProfile 启动 PowerShell 更快
                "chcp 65001 >NUL && powershell.exe -NoProfile -Command \"" + powerShellCommand + "\""
        };

        StringBuilder jsonOutput = new StringBuilder();
        StringBuilder errorOutput = new StringBuilder();

        try {
            Process process = Runtime.getRuntime().exec(command);

            Charset consoleEncoding = StandardCharsets.UTF_8;

            // 捕获标准输出流
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), consoleEncoding))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    jsonOutput.append(line);
                }
            }

            // 捕获错误流
            try (BufferedReader errorReader = new BufferedReader(
                    new InputStreamReader(process.getErrorStream(), consoleEncoding))) {
                String errorLine;
                while ((errorLine = errorReader.readLine()) != null) {
                    errorOutput.append(errorLine).append("\n");
                }
            }

            // 等待进程结束，设置一个超时
            if (!process.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                process.destroy();
                log.error("PowerShell 命令执行超时！");
                return LogQueryResponse.EMPTY;
            }

            // 检查是否有错误
            if (process.exitValue() != 0) {
                log.error("PowerShell 命令执行失败，退出码: {}", process.exitValue());
                log.error("错误详情: {}", errorOutput);
                return LogQueryResponse.EMPTY;
            }

            String json = jsonOutput.toString();
            if (json.isEmpty()) {
                log.info("未找到匹配的日志条目。");
                return LogQueryResponse.EMPTY;
            }

            // PowerShell 在返回单个对象时也会将其包裹在数组中，所以这里始终解析为 List
            List<WinEventLogEntry> entries = objectMapper.readValue(json, new TypeReference<>() {});

            boolean hasMore = entries.size() > queryRequest.getMaxEvents();

            if (hasMore) {
                entries = entries.subList(0, queryRequest.getMaxEvents());
            }

            return new LogQueryResponse(entries, hasMore);

        } catch (Exception e) {
            log.error("读取 Windows 事件日志时发生异常", e);
            log.error(e.getMessage(), e);

            return LogQueryResponse.EMPTY;
        }
    }

    public static class EventViewerFilterCommandBuilder {

        // yyyy-MM-ddTHH:mm:ss
        private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

        private final StringBuilder filterBuilder;
        private int maxEvents;
        private Integer startHoursAgo;
        private Integer endHoursAgo;
        private boolean logNameSet = false;

        private EventViewerFilterCommandBuilder() {
            this.filterBuilder = new StringBuilder("$OutputEncoding = [System.Text.Encoding]::UTF8; Get-WinEvent -FilterHashtable @{");
            this.maxEvents = 10;
            this.startHoursAgo = null;
            this.endHoursAgo = null;
        }

        public static EventViewerFilterCommandBuilder builder() {
            return new EventViewerFilterCommandBuilder();
        }

        public String build() {
            if (!logNameSet) {
                throw new IllegalStateException("必须设置日志名称 (logName)");
            }

            if (filterBuilder.length() >= 2) {
                filterBuilder.setLength(filterBuilder.length() - 2);
            }
            filterBuilder.append("} -MaxEvents ").append(maxEvents + 1).append(" | ConvertTo-Json");

            return filterBuilder.toString();
        }

        public EventViewerFilterCommandBuilder logName(String logName) {
            if (logName == null || logName.isEmpty()) {
                return this;
            }

            if (!ReUtils.isPlainText(logName)) {
                log.warn("日志名称包含非法字符，已被忽略: {}", logName);
                return this;
            }

            this.logNameSet = true;
            filterBuilder.append("LogName='").append(logName).append("'; ");
            return this;
        }

        public EventViewerFilterCommandBuilder levels(List<LogLevel> levels) {
            if (levels == null || levels.isEmpty()) {
                return this;
            }

            String levelString = String.join(",",
                    levels.stream().map(LogLevel::getValue).map(String::valueOf).toArray(String[]::new)
            );
            filterBuilder.append("Level=@(").append(levelString).append("); ");
            return this;
        }

        public EventViewerFilterCommandBuilder maxEvents(int maxEvents) {
            this.maxEvents = maxEvents;
            return this;
        }

        public EventViewerFilterCommandBuilder startHoursAgo(Integer startHoursAgo) {
            if (this.endHoursAgo != null && startHoursAgo != null && startHoursAgo < this.endHoursAgo) {
                throw new IllegalArgumentException("开始时间点不能早于结束时间点: startHoursAgo 应当大于或等于 endHoursAgo");
            }
            this.startHoursAgo = startHoursAgo;
            if (startHoursAgo != null && startHoursAgo > 0) {
                String timeStr = LocalDateTime.now().minusHours(startHoursAgo).truncatedTo(ChronoUnit.SECONDS).format(ISO_FORMATTER);
                filterBuilder.append("StartTime=[datetime]'").append(timeStr).append("'; ");
            }
            return this;
        }

        public EventViewerFilterCommandBuilder endHoursAgo(Integer endHoursAgo) {
            if (this.startHoursAgo != null && endHoursAgo != null && endHoursAgo > this.startHoursAgo) {
                throw new IllegalArgumentException("结束时间点不能晚于开始时间点: endHoursAgo 应当小于或等于 startHoursAgo");
            }
            this.endHoursAgo = endHoursAgo;
            if (endHoursAgo != null && endHoursAgo > 0) {
                String timeStr = LocalDateTime.now().minusHours(endHoursAgo).truncatedTo(ChronoUnit.SECONDS).format(ISO_FORMATTER);
                filterBuilder.append("EndTime=[datetime]'").append(timeStr).append("'; ");
            }
            return this;
        }

    }

}

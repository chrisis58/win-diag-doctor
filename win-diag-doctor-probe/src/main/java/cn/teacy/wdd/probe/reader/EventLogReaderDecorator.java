package cn.teacy.wdd.probe.reader;

import cn.teacy.wdd.common.entity.WinEventLogEntry;
import cn.teacy.wdd.protocol.command.LogQueryRequest;
import cn.teacy.wdd.protocol.response.LogQueryResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 事件日志读取器的装饰器
 * 用于清理和格式化日志条目
 */
@Primary
@Component
public class EventLogReaderDecorator implements IWinEventLogReader {

    private static final Pattern NON_DIGITS = Pattern.compile("\\D+");
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final IWinEventLogReader delegate;

    public EventLogReaderDecorator(@Qualifier(IWinEventLogReader.BEAN_NAME) IWinEventLogReader delegate) {
        this.delegate = delegate;
    }

    @Override
    public LogQueryResponse readEventLogs(LogQueryRequest queryRequest) {
        LogQueryResponse queryResponse = delegate.readEventLogs(queryRequest);

        // TODO: 添加数据脱敏等逻辑
        List<WinEventLogEntry> logEntries = queryResponse.getEntries().stream()
                .peek(it -> {
                    String cleaned = it.getMessage().trim();

                    if (cleaned.startsWith(":")) {
                        cleaned = cleaned.substring(1).trim();
                    }

                    String timestampStr = NON_DIGITS.matcher(it.getTimeCreated()).replaceAll("");
                    if (!timestampStr.isEmpty()) {
                        try {
                            long timestamp = Long.parseLong(timestampStr);
                            Instant instant = Instant.ofEpochMilli(timestamp);
                            LocalDateTime localDateTime = LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
                            String formattedTime = localDateTime.format(FORMATTER);
                            it.setTimeCreated(formattedTime);
                        } catch (NumberFormatException e) {
                            // Ignore parsing errors and keep the original timeCreated
                        }
                    }

                    it.setMessage(cleaned);
                }).toList();
        queryResponse.setEntries(logEntries);

        return queryResponse;
    }
}

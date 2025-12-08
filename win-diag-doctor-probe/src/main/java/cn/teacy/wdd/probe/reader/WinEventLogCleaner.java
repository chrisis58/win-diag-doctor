package cn.teacy.wdd.probe.reader;

import cn.teacy.wdd.common.entity.WinEventLogEntry;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.regex.Pattern;

@Component
public class WinEventLogCleaner implements IWinEventLogCleaner {

    private static final Pattern NON_DIGITS = Pattern.compile("\\D+");
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public List<WinEventLogEntry> handle(List<WinEventLogEntry> logEntries) {
        // TODO: 后续添加数据脱敏等处理逻辑
        return logEntries.stream()
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
    }
}

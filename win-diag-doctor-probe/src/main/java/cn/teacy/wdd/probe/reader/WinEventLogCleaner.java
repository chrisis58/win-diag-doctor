package cn.teacy.wdd.probe.reader;

import cn.teacy.wdd.common.entity.WinEventLogEntry;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class WinEventLogCleaner implements IWinEventLogCleaner {

    @Override
    public List<WinEventLogEntry> handle(List<WinEventLogEntry> logEntries) {
        // TODO: 后续添加数据脱敏等处理逻辑
        return logEntries.stream()
                .peek(it -> {
                    String cleaned = it.getMessage().trim();

                    if (cleaned.startsWith(":")) {
                        cleaned = cleaned.substring(1).trim();
                    }

                    it.setMessage(cleaned);
                }).toList();
    }
}

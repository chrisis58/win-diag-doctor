package cn.teacy.wdd.controller;

import cn.teacy.wdd.common.constants.LogNames;
import cn.teacy.wdd.common.entity.WinEventLogEntry;
import cn.teacy.wdd.common.enums.LogLevel;
import cn.teacy.wdd.protocol.WsMessageContext;
import cn.teacy.wdd.protocol.command.LogQueryRequest;
import cn.teacy.wdd.service.LogQueryService;
import com.felipestanzani.jtoon.Delimiter;
import com.felipestanzani.jtoon.EncodeOptions;
import com.felipestanzani.jtoon.JToon;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/analyze/logs")
public class LogAnalyseController {

    private final ChatClient chatClient;
    private final LogQueryService logQueryService;

    @PostMapping("/resolve")
    public String analyse(@RequestBody WsMessageContext queryContext) {
        String toonEncoded = JToon.encode(queryContext, new EncodeOptions(2, Delimiter.PIPE, true));

        log.debug(toonEncoded);

        return chatClient.prompt(
                "分析以下 Windows 事件日志条目，并提供潜在问题或值得注意事件的摘要 :\n"
                        + toonEncoded
                ).call()
                .content();
    }

    @GetMapping("/query/{probeId}")
    public ResponseEntity<List<WinEventLogEntry>> queryLogs(@PathVariable("probeId") String probeId) {
        List<WinEventLogEntry> logEntries = logQueryService.queryLog(probeId, LogQueryRequest.builder()
                .levels(List.of(LogLevel.CRITICAL, LogLevel.ERROR))
                .logName(LogNames.SYSTEM)
                .maxEvents(10)
                .build());

        return ResponseEntity.ok(logEntries);
    }

}

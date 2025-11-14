package cn.teacy.wdd.controller;

import cn.teacy.wdd.common.dto.LogQueryContext;
import cn.teacy.wdd.common.dto.WinEventLogEntry;
import com.felipestanzani.jtoon.JToon;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/analyze/logs")
public class LogAnalyseController {

    private final ChatClient chatClient;

    @PostMapping("/resolve")
    public String analyse(@RequestBody LogQueryContext queryContext) {
        return chatClient.prompt(
                "分析以下 Windows 事件日志条目，并提供潜在问题或值得注意事件的摘要 :\n"
                        + JToon.encode(queryContext)
                ).call()
                .content();
    }

}

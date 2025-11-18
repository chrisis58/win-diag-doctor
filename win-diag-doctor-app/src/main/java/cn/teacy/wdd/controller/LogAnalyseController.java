package cn.teacy.wdd.controller;

import cn.teacy.wdd.protocol.WsMessageContext;
import com.felipestanzani.jtoon.Delimiter;
import com.felipestanzani.jtoon.EncodeOptions;
import com.felipestanzani.jtoon.JToon;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/analyze/logs")
public class LogAnalyseController {

    private final ChatClient chatClient;

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

}

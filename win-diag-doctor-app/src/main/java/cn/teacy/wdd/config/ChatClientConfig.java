package cn.teacy.wdd.config;

import cn.teacy.wdd.protocol.command.LogQueryRequest;
import cn.teacy.wdd.service.LogQueryService;
import com.alibaba.cloud.ai.agent.studio.loader.AgentLoader;
import com.alibaba.cloud.ai.graph.agent.BaseAgent;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.felipestanzani.jtoon.Delimiter;
import com.felipestanzani.jtoon.EncodeOptions;
import com.felipestanzani.jtoon.JToon;
import org.jetbrains.annotations.NotNull;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.function.BiFunction;

@Configuration
public class ChatClientConfig {

    @Bean
    public ChatClient chatClient(ChatModel chatModel) {
        return ChatClient.builder(chatModel)
                .build();
    }

    @Bean
    public AgentLoader agentLoader(ChatClient chatClient, LogQueryService logQueryService) {

        ToolCallback toolCallback = FunctionToolCallback
                .builder("eventLogQuery", new BiFunction<LogQueryRequest, ToolContext, String>() {
                    @Override
                    public String apply(LogQueryRequest queryRequest, ToolContext toolContext) {
                        // TODO: 从对话上下文中获取 pid
                        return JToon.encode(
                                logQueryService.queryLog(System.getenv("WDD_PROBE_ID"), queryRequest),
                                new EncodeOptions(2, Delimiter.PIPE, true)
                        );
                    }
                }).description("""
                本工具用于查询 Windows 事件日志。接受一个 LogQueryRequest 对象作为输入，返回符合查询条件的 Windows 事件日志条目列表。
                你可以使用此工具来获取特定日志名称、级别和时间范围内的事件日志，以帮助诊断和分析 Windows 系统中的问题。
                """)
                .inputType(LogQueryRequest.class)
                .build();

        ReactAgent agent = ReactAgent.builder()
                .chatClient(chatClient)
                .tools(toolCallback)
                .name("wdd-agent")
                .build();

        return new AgentLoader() {
            @NotNull
            @Override
            public List<String> listAgents() {
                return List.of("wdd-agent");
            }

            @Override
            public BaseAgent loadAgent(String name) {
                return agent;
            }
        };

    }

}

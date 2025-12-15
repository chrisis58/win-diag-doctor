package cn.teacy.wdd.config;

import cn.teacy.wdd.agent.prompt.PromptIdentifier;
import cn.teacy.wdd.agent.prompt.PromptLoader;
import cn.teacy.wdd.agent.tools.annotations.DiagnosticTool;
import com.alibaba.cloud.ai.agent.studio.loader.AgentLoader;
import com.alibaba.cloud.ai.graph.agent.BaseAgent;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.agent.interceptor.toolerror.ToolErrorInterceptor;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
import org.jetbrains.annotations.NotNull;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class ChatClientConfig {

    @Bean
    public ChatClient chatClient(ChatModel chatModel) {
        return ChatClient.builder(chatModel)
                .build();
    }

    @Bean
    public AgentLoader agentLoader(
            PromptLoader promptLoader,
            ChatClient chatClient,
            @DiagnosticTool List<ToolCallback> diagnosticToolCallbacks
    ) {

        ReactAgent agent = ReactAgent.builder()
                .systemPrompt(promptLoader.loadPrompt(PromptIdentifier.CHAT_AGENT_SYS_PROMPT))
                .chatClient(chatClient)
                .tools(diagnosticToolCallbacks)
                .interceptors(new ToolErrorInterceptor())
                .saver(new MemorySaver())
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

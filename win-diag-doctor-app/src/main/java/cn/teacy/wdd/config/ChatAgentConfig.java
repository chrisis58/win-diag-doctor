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
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class ChatAgentConfig {

    @Value("${spring.ai.openai.api-key}")
    private String apiKey;

    @Value("${spring.ai.openai.base-url}")
    private String baseUrl;

    @Value("${spring.ai.openai.chat.options.model}")
    private String defaultModel;

    @Bean
    public ChatModel chatModel() {
        return OpenAiChatModel.builder()
                .defaultOptions(
                        OpenAiChatOptions.builder()
                                .model(defaultModel)
                                .build()
                ).openAiApi(
                        OpenAiApi.builder()
                                .apiKey(apiKey)
                                .baseUrl(baseUrl)
                                .build()
                ).build();
    }

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

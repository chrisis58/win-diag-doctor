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
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.List;

@Configuration
public class ChatAgentConfig {

    @Value("${spring.ai.openai.api-key}")
    private String apiKey;

    @Value("${spring.ai.openai.base-url}")
    private String baseUrl;

    @Value("${spring.ai.openai.chat.options.model}")
    private String defaultModel;

    @Value("${wdd.agent.think-model}")
    private String thinkModel;

    @Value("${wdd.agent.flash-model}")
    private String flashModel;

    @Bean
    public OpenAiApi openAiApi() {
        return OpenAiApi.builder()
                .apiKey(apiKey)
                .baseUrl(baseUrl)
                .build();
    }

    @Bean
    @Primary
    public ChatModel defaultChatModel(OpenAiApi openAiApi) {
        return OpenAiChatModel.builder()
                .openAiApi(openAiApi)
                .defaultOptions(OpenAiChatOptions.builder()
                        .model(defaultModel)
                        .temperature(0.7)
                        .build())
                .build();
    }

    @Bean("thinkChatModel")
    public ChatModel thinkChatModel(OpenAiApi openAiApi) {
        return OpenAiChatModel.builder()
                .openAiApi(openAiApi)
                .defaultOptions(OpenAiChatOptions.builder()
                        .model(thinkModel)
                        .temperature(0.8)
                        .build())
                .build();
    }

    @Bean("flashChatModel")
    public ChatModel flashChatModel(OpenAiApi openAiApi) {
        return OpenAiChatModel.builder()
                .openAiApi(openAiApi)
                .defaultOptions(OpenAiChatOptions.builder()
                        .model(flashModel)
                        .temperature(0.1)
                        .build())
                .build();
    }

    @Bean
    @Primary
    public ChatClient chatClient(ChatModel chatModel) {
        return ChatClient.builder(chatModel)
                .build();
    }

    @Bean("thinkChatClient")
    public ChatClient thinkChatClient(@Qualifier("thinkChatModel") ChatModel thinkChatModel) {
        return ChatClient.builder(thinkChatModel).build();
    }

    @Bean("flashChatClient")
    public ChatClient flashChatClient(@Qualifier("flashChatModel") ChatModel flashChatModel) {
        return ChatClient.builder(flashChatModel).build();
    }

    @Bean
    @Primary
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

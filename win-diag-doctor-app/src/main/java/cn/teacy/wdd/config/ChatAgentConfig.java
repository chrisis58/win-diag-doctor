package cn.teacy.wdd.config;

import cn.teacy.ai.annotation.CompiledFrom;
import cn.teacy.wdd.agent.graph.LogAnalyseGraphComposer;
import cn.teacy.wdd.config.properties.WddProperties;
import com.alibaba.cloud.ai.agent.studio.loader.AgentLoader;
import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.agent.BaseAgent;
import com.alibaba.cloud.ai.graph.internal.node.Node;
import com.alibaba.cloud.ai.graph.internal.node.SubCompiledGraphNode;
import org.jetbrains.annotations.NotNull;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.List;

@Configuration
public class ChatAgentConfig {

    @Bean
    public OpenAiApi openAiApi(WddProperties properties) {
        return OpenAiApi.builder()
                .apiKey(properties.getAi().getApiKey())
                .baseUrl(properties.getAi().getBaseUrl())
                .build();
    }

    @Bean
    @Primary
    public ChatModel defaultChatModel(OpenAiApi openAiApi, WddProperties properties) {
        return OpenAiChatModel.builder()
                .openAiApi(openAiApi)
                .defaultOptions(OpenAiChatOptions.builder()
                        .model(properties.getAi().getDefaultModel())
                        .temperature(0.7)
                        .build())
                .build();
    }

    @Bean("thinkChatModel")
    public ChatModel thinkChatModel(OpenAiApi openAiApi, WddProperties properties) {
        return OpenAiChatModel.builder()
                .openAiApi(openAiApi)
                .defaultOptions(OpenAiChatOptions.builder()
                        .model(properties.getAi().getThinkModel())
                        .temperature(0.8)
                        .build())
                .build();
    }

    @Bean("flashChatModel")
    public ChatModel flashChatModel(OpenAiApi openAiApi, WddProperties properties) {
        return OpenAiChatModel.builder()
                .openAiApi(openAiApi)
                .defaultOptions(OpenAiChatOptions.builder()
                        .model(properties.getAi().getFlashModel())
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
            @CompiledFrom(LogAnalyseGraphComposer.class) CompiledGraph logAnalyseGraph
    ) {
        BaseAgent agent = new BaseAgent(
                "wdd-agent",
                "Agent for Win Diagnostic Doctor",
                false,
                false,
                LogAnalyseGraphComposer.KEY_ANALYSE_REPORT,
                null
        ) {

            @Override
            public Node asNode(boolean includeContents, boolean returnReasoningContents, String outputKeyToParent) {
                return new SubCompiledGraphNode("", logAnalyseGraph);
            }

            @Override
            protected StateGraph initGraph() {
                return logAnalyseGraph.stateGraph;
            }
        };

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

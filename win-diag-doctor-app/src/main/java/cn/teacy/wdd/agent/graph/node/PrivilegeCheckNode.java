package cn.teacy.wdd.agent.graph.node;

import cn.teacy.wdd.agent.graph.LogAnalyseGraphComposer;
import cn.teacy.wdd.agent.prompt.PromptIdentifier;
import cn.teacy.wdd.agent.prompt.PromptLoader;
import cn.teacy.wdd.agent.service.IUserContextProvider;
import cn.teacy.wdd.agent.utils.ModelChatUtils;
import cn.teacy.wdd.common.entity.UserContext;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.action.AsyncNodeActionWithConfig;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.felipestanzani.jtoon.JToon;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Component
@RequiredArgsConstructor
public class PrivilegeCheckNode implements AsyncNodeActionWithConfig {

    private final PromptLoader promptLoader;
    private final IUserContextProvider userContextProvider;
    private final ObjectMapper objectMapper;
    private final ChatClient flashChatClient;

    private static final String KEY_QUERY = LogAnalyseGraphComposer.KEY_QUERY;
    private static final String KEY_PRIVILEGE_QUALIFIED = LogAnalyseGraphComposer.KEY_PRIVILEGE_QUALIFIED;

    record PrivilegeCheckerQuery(String query, UserContext userContext) {}

    @Override
    public CompletableFuture<Map<String, Object>> apply(OverAllState state, RunnableConfig config) {
        Optional<Object> query = state.value(KEY_QUERY);
        if (query.isEmpty()) {
            return CompletableFuture.completedFuture(
                    Map.of(KEY_PRIVILEGE_QUALIFIED, "Cannot perform privilege check: query is missing.")
            );
        }

        String queryString = query.toString();

        UserContext userContext = userContextProvider.getUserContext(state, config);

        String messagesString = promptLoader.read(PromptIdentifier.PRIVILEGE_CHECKER_MESSAGES);
        List<Message> messages = Collections.emptyList();
        try {
            messages = objectMapper.readValue(messagesString, new TypeReference<>() {
            });
        } catch (JsonProcessingException e) {
            // pass
        }

        ChatResponse response = flashChatClient.prompt()
                .messages(messages)
                .system(promptLoader.loadPrompt(PromptIdentifier.PRIVILEGE_CHECKER_SYS_PROMPT))
                .user(JToon.encode(new PrivilegeCheckerQuery(queryString, userContext)))
                .call()
                .chatResponse();

        String output = ModelChatUtils.extractContent(response, "权限服务响应异常");

        if (output.length() < 10 && output.toLowerCase().contains("true")) {
            output = "true";
        }

        return CompletableFuture.completedFuture(
                Map.of(KEY_PRIVILEGE_QUALIFIED, output)
        );
    }
}

package cn.teacy.wdd.agent.graph.node;

import com.alibaba.cloud.ai.graph.GraphResponse;
import com.alibaba.cloud.ai.graph.NodeOutput;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.action.AsyncNodeActionWithConfig;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.exception.GraphRunnerException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static cn.teacy.wdd.agent.graph.LogAnalyseGraphComposer.*;

@Slf4j
@Component
public class ReactChatNode implements AsyncNodeActionWithConfig {

    private final ReactAgent reactAgent;

    public ReactChatNode(@Qualifier("thinkChatModel") ChatModel thinkChatModel) {
        this.reactAgent = ReactAgent.builder()
                .name("wdd-chat-agent")
                .model(thinkChatModel)
                .instruction("请结合用户输入和工具返回的信息，进行分析并给出回复。")
                .build();
    }

    @Override
    public CompletableFuture<Map<String, Object>> apply(OverAllState state, RunnableConfig config) {
        Flux<GraphResponse<NodeOutput>> flux;
        try {
            flux = reactAgent.stream("用户输入: " + state.value(KEY_QUERY, "") + ", 工具返回: " + state.value(KEY_ANALYSE_REPORT, ""))
                    .map(GraphResponse::of);
        } catch (GraphRunnerException e) {
            log.error(e.getMessage(), e);
            flux = Flux.just(GraphResponse.error(e));
        }
        return CompletableFuture.completedFuture(Map.of(KEY_MESSAGES, flux));
    }
}

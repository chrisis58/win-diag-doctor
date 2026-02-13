package cn.teacy.wdd.agent.graph.node;

import cn.teacy.wdd.agent.graph.LogAnalyseGraphComposer;
import cn.teacy.wdd.agent.prompt.PromptIdentifier;
import cn.teacy.wdd.agent.prompt.PromptLoader;
import cn.teacy.wdd.agent.utils.ModelChatUtils;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.action.AsyncNodeActionWithConfig;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Component
public class LogAnalystNode implements AsyncNodeActionWithConfig {

    private final PromptLoader promptLoader;
    private final ObjectMapper xmlMapper;
    private final ChatClient thinkChatClient;
    private final ObjectMapper objectMapper;

    public LogAnalystNode(
            PromptLoader promptLoader,
            @Qualifier("xmlMapper") ObjectMapper xmlMapper,
            @Qualifier("thinkChatClient") ChatClient thinkChatClient,
            ObjectMapper objectMapper
    ) {
        this.promptLoader = promptLoader;
        this.xmlMapper = xmlMapper;
        this.thinkChatClient = thinkChatClient;
        this.objectMapper = objectMapper;
    }

    private static final String KEY_ITERATION_COUNT = LogAnalyseGraphComposer.KEY_ITERATION_COUNT;
    private static final String KEY_EVENT_LOG_RESULT = LogAnalyseGraphComposer.KEY_EVENT_LOG_RESULT;
    private static final String KEY_QUERY = LogAnalyseGraphComposer.KEY_QUERY;
    private static final String KEY_ANALYSE_REPORT = LogAnalyseGraphComposer.KEY_ANALYSE_REPORT;
    private static final String KEY_EXECUTOR_INSTRUCTION = LogAnalyseGraphComposer.KEY_EXECUTOR_INSTRUCTION;
    private static final String KEY_MESSAGES = LogAnalyseGraphComposer.KEY_MESSAGES;


    record AnalyseReport(
            @JsonProperty(required = true) String report,
            @JsonProperty() String newInstruction
    ) {}

    private static final AnalyseReport EMPTY_REPORT = new AnalyseReport("", null);
    private static final PromptTemplate analystPrompt = new PromptTemplate("""
                Query: {query}
                Transcript:
                {transcript}
                """);

    @Override
    @SuppressWarnings("unchecked")
    public CompletableFuture<Map<String, Object>> apply(OverAllState state, RunnableConfig config) {
        int iterationCount = (int) state.value(KEY_ITERATION_COUNT).orElse(0);

        assert state.value(KEY_EVENT_LOG_RESULT).isPresent();
        List<LogAnalyseGraphComposer.ExecutionRecord> executionRecords =
                (List<LogAnalyseGraphComposer.ExecutionRecord>) state.value(KEY_EVENT_LOG_RESULT, List.class).orElseThrow();
        LogAnalyseGraphComposer.TranscriptWrapper transcript = new LogAnalyseGraphComposer.TranscriptWrapper(executionRecords, iterationCount);

        String transcriptString;
        try {
            transcriptString = xmlMapper.writeValueAsString(transcript);
        } catch (JsonProcessingException e) {
            transcriptString = "<transcript></transcript>";
        }

        assert state.value(KEY_QUERY).isPresent();
        String queryString = state.value(KEY_QUERY).get().toString();


        BeanOutputConverter<AnalyseReport> converter = new BeanOutputConverter<>(AnalyseReport.class);

        ChatResponse response = thinkChatClient.prompt()
                .system(promptLoader.render(PromptIdentifier.LOG_ANALYSE_ANALYST_SYS_PROMPT, Map.of(
                        "format", converter.getFormat()
                )))
                .user(analystPrompt.render(Map.of(
                        "query", queryString,
                        "transcript", transcriptString
                ))).call()
                .chatResponse();

        AnalyseReport report = ModelChatUtils.extractOutput(response, EMPTY_REPORT, objectMapper);

        return CompletableFuture.completedFuture(Map.of(
                KEY_ANALYSE_REPORT, report.report(),
                KEY_EXECUTOR_INSTRUCTION, report.newInstruction() != null ? report.newInstruction() : "",
                KEY_ITERATION_COUNT, iterationCount + 1,
                KEY_MESSAGES, new AssistantMessage(report.report)
        ));
    }
}

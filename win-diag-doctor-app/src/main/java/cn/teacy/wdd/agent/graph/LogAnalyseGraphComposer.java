package cn.teacy.wdd.agent.graph;

import cn.teacy.wdd.agent.common.GraphKey;
import cn.teacy.wdd.agent.common.GraphNode;
import cn.teacy.wdd.agent.node.InterruptibleNodeAction;
import cn.teacy.wdd.agent.prompt.PromptIdentifier;
import cn.teacy.wdd.agent.prompt.PromptLoader;
import cn.teacy.wdd.agent.service.IUserContextProvider;
import cn.teacy.wdd.agent.tools.annotations.DiagnosticTool;
import cn.teacy.wdd.agent.utils.ModelChatUtils;
import cn.teacy.wdd.common.entity.UserContext;
import com.alibaba.cloud.ai.graph.*;
import com.alibaba.cloud.ai.graph.action.AsyncNodeActionWithConfig;
import com.alibaba.cloud.ai.graph.action.Command;
import com.alibaba.cloud.ai.graph.action.InterruptionMetadata;
import com.alibaba.cloud.ai.graph.checkpoint.config.SaverConfig;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import com.alibaba.cloud.ai.graph.state.strategy.AppendStrategy;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import com.felipestanzani.jtoon.JToon;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static com.alibaba.cloud.ai.graph.agent.tools.ToolContextConstants.AGENT_CONFIG_CONTEXT_KEY;

@Component
public class LogAnalyseGraphComposer extends AbstractGraphComposer {

    private final MemorySaver saver = new MemorySaver();

    @GraphKey
    public static final String KEY_QUERY = "query";

    @GraphKey
    private static final String KEY_PRIVILEGE_QUALIFIED = "privilege-qualified";

    @GraphKey
    public static final String KEY_EXECUTION_PLAN = "execution-plan";

    @GraphKey
    public static final String KEY_EXECUTOR_INSTRUCTION = "executor-instruction";

    @GraphKey(strategy = AppendStrategy.class)
    private static final String KEY_EVENT_LOG_RESULT = "event-log-result";

    @GraphKey
    private static final String KEY_ITERATION_COUNT = "iteration-count";

    @GraphKey
    public static final String KEY_ANALYSE_REPORT = "analyse-report";

    @GraphNode("privilege-checker")
    private final AsyncNodeActionWithConfig privilegeCheckNode;

    @GraphNode("pre-check-result-handler")
    private final InterruptibleNodeAction privilegeCheckResultHandleNode;

    @GraphNode("execution-planner")
    private final AsyncNodeActionWithConfig executionPlanner;

    @GraphNode("plan-executor")
    private final AsyncNodeActionWithConfig planExecutor;

    @GraphNode("analyst")
    private final AsyncNodeActionWithConfig analyst;

    record PrivilegeCheckerQuery(String query, UserContext userContext) {}

    public record PlannerResponse(
            @JsonProperty(required = true) String executionPlan,
            @JsonProperty(required = true) String executorInstruction
    ) {}

    record ExecutionRecord(
            @JacksonXmlProperty(localName = "action") String action,
            @JacksonXmlProperty(localName = "result") String result
    ) {}

    @JacksonXmlRootElement(localName = "transcript")
    record TranscriptWrapper(
            @JacksonXmlElementWrapper(useWrapping = false)
            @JacksonXmlProperty(localName = "step")
            List<ExecutionRecord> records,

            @JacksonXmlProperty(isAttribute = true)
            int interactionCount
    ) {}

    record AnalyseReport(
            @JsonProperty(required = true) String report,
            @JsonProperty() String newInstruction
    ) {}

    private static final AnalyseReport EMPTY_REPORT = new AnalyseReport("", null);

    private static final PlannerResponse EMPTY_RESPONSE = new PlannerResponse(null, null);

    /** 在此构造器中初始化各个节点 */
    public LogAnalyseGraphComposer(
            @Qualifier("flashChatClient") ChatClient flashChatClient,
            @Qualifier("thinkChatClient") ChatClient thinkChatClient,
            IUserContextProvider userContextProvider,
            PromptLoader promptLoader,
            @DiagnosticTool List<ToolCallback> diagnosticTools,
            ObjectMapper objectMapper,
            XmlMapper xmlMapper
    ) {

        this.privilegeCheckNode = (state, config) -> {

            Optional<Object> query = state.value(KEY_QUERY);
            if (query.isEmpty()) {
                return CompletableFuture.completedFuture(
                        Map.of(KEY_PRIVILEGE_QUALIFIED, "Cannot perform privilege check: query is missing.")
                );
            }

            String queryString = query.toString();

            UserContext userContext = userContextProvider.getUserContext(state, config);

            ChatResponse response = flashChatClient.prompt()
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
        };

        this.privilegeCheckResultHandleNode = (nodeId, state, config) -> {

            Optional<Object> value = state.value(KEY_PRIVILEGE_QUALIFIED);

            if (value.isPresent() && String.valueOf(Boolean.TRUE).equals(value.get())) {
                // Privilege check passed
                return Optional.empty();
            }

            InterruptionMetadata interruption = InterruptionMetadata.builder(nodeId, state)
                    .addMetadata("message", value.isPresent()
                            ? value.get().toString()
                            : "Privilege check failed: insufficient permissions to perform log analysis.")
                    .addMetadata("node", nodeId)
                    .build();

            return Optional.of(interruption);
        };

        this.executionPlanner = (state, config) -> {

            Optional<Object> query = state.value(KEY_QUERY);
            assert query.isPresent();
            String queryString = query.get().toString();

            BeanOutputConverter<PlannerResponse> converter = new BeanOutputConverter<>(PlannerResponse.class);

            ChatResponse response = thinkChatClient.prompt()
                    .system(promptLoader.render(PromptIdentifier.LOG_ANALYSE_EXECUTION_PLANNER_SYS_PROMPT,
                            Map.of("format", converter.getFormat())
                    ))
                    .user(JToon.encode(Map.of(
                            "query", queryString,
                            "available_tools", diagnosticTools.stream().map(ToolCallback::getToolDefinition).map(ToolDefinition::description).toList()
                    )))
                    .call()
                    .chatResponse();

            PlannerResponse plannerResponse = ModelChatUtils.extractOutput(response, EMPTY_RESPONSE, objectMapper);

            if (plannerResponse.executionPlan() == null || plannerResponse.executionPlan().isBlank()) {
                return CompletableFuture.completedFuture(
                        Map.of(KEY_EXECUTION_PLAN, "ERROR")
                );
            }

            return CompletableFuture.completedFuture(Map.of(
                    KEY_EXECUTION_PLAN, plannerResponse.executionPlan(),
                    KEY_EXECUTOR_INSTRUCTION, plannerResponse.executorInstruction()
            ));
        };

        this.planExecutor = (state, config) -> {

            Optional<Object> value = state.value(KEY_EXECUTOR_INSTRUCTION);
            assert value.isPresent();
            String executionInstruction = value.get().toString();

            ChatResponse response = flashChatClient.prompt()
                    .system(promptLoader.loadPrompt(PromptIdentifier.LOG_ANALYSE_PLAN_EXECUTOR_SYS_PROMPT))
                    .user(executionInstruction)
                    .toolCallbacks(diagnosticTools)
                    .toolContext(Map.of(AGENT_CONFIG_CONTEXT_KEY, config))
                    .call()
                    .chatResponse();

            String content = ModelChatUtils.extractContent(response, "ERROR");

            return CompletableFuture.completedFuture(Map.of(
                    KEY_EVENT_LOG_RESULT, new ExecutionRecord(executionInstruction, content),
                    KEY_EXECUTOR_INSTRUCTION, ""
            ));

        };

        final PromptTemplate analystPrompt = new PromptTemplate("""
                Query: {query}
                Transcript:
                {transcript}
                """);

        this.analyst = (state, config) -> {

            int iterationCount = (int) state.value(KEY_ITERATION_COUNT).orElse(0);

            assert state.value(KEY_EVENT_LOG_RESULT).isPresent();
            List<ExecutionRecord> executionRecords = (List<ExecutionRecord>) state.value(KEY_EVENT_LOG_RESULT).get();
            TranscriptWrapper transcript = new TranscriptWrapper(executionRecords, iterationCount);

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
                    KEY_ITERATION_COUNT, iterationCount + 1
            ));
        };

    }

    @Override
    protected void addEdges(StateGraph builder) throws GraphStateException {
        builder.addEdge(StateGraph.START, idOf(privilegeCheckNode));
        builder.addEdge(idOf(privilegeCheckNode), idOf(privilegeCheckResultHandleNode));
        builder.addEdge(idOf(privilegeCheckResultHandleNode), idOf(executionPlanner));
        builder.addEdge(idOf(executionPlanner), idOf(planExecutor));
        builder.addEdge(idOf(planExecutor), idOf(analyst));

        builder.addConditionalEdges(idOf(analyst), (OverAllState state, RunnableConfig config) -> {
                int iterationCount = (int) state.value(KEY_ITERATION_COUNT).orElse(0);
                String newInstruction = state.value(KEY_EXECUTOR_INSTRUCTION).orElse("").toString();

                String next;

                if (iterationCount < 3 && newInstruction != null && !newInstruction.isBlank()) {
                    next = "true";
                } else {
                    next = "false";
                }

                return CompletableFuture.completedFuture(new Command(next, Map.of(
                        KEY_ITERATION_COUNT, iterationCount + 1
                )));
            },
            Map.of(
                    "true", idOf(planExecutor),
                    "false", StateGraph.END
            ));
    }

    @Override
    protected CompileConfig compileConfig() {
        return CompileConfig.builder()
                .saverConfig(SaverConfig.builder()
                        .register(saver)
                        .build())
                .build();
    }

    @Bean
    public CompiledGraph logAnalyseGraph() {
        return compose();
    }

}

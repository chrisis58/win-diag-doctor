package cn.teacy.wdd.agent.graph;

import cn.teacy.ai.annotation.*;
import cn.teacy.wdd.agent.graph.dispatcher.LoopAnalystDispatcher;
import cn.teacy.wdd.agent.graph.dispatcher.PrivilegeCheckDispatcher;
import cn.teacy.wdd.agent.graph.node.ExecutionPlannerNode;
import cn.teacy.wdd.agent.graph.node.LogAnalystNode;
import cn.teacy.wdd.agent.graph.node.PlanExecutorNode;
import cn.teacy.wdd.agent.graph.node.PrivilegeCheckNode;
import com.alibaba.cloud.ai.graph.*;
import com.alibaba.cloud.ai.graph.checkpoint.config.SaverConfig;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
import com.alibaba.cloud.ai.graph.state.strategy.AppendStrategy;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

import java.util.List;

@GraphComposer(description = "Workflow to analyze logs based on user query")
public class LogAnalyseGraphComposer {

    private final MemorySaver saver = new MemorySaver();

    @GraphKey
    public static final String KEY_QUERY = "query";

    @GraphKey
    public static final String KEY_PRIVILEGE_QUALIFIED = "privilege-qualified";

    @GraphKey
    public static final String KEY_EXECUTION_PLAN = "execution-plan";

    @GraphKey
    public static final String KEY_EXECUTOR_INSTRUCTION = "executor-instruction";

    @GraphKey(strategy = AppendStrategy.class)
    public static final String KEY_EVENT_LOG_RESULT = "event-log-result";

    @GraphKey
    public static final String KEY_ITERATION_COUNT = "iteration-count";

    @GraphKey
    public static final String KEY_ANALYSE_REPORT = "analyse-report";

    private static final String NODE_PRIVILEGE_CHECKER = "privilege-checker";
    private static final String NODE_EXECUTION_PLANNER = "execution-planner";
    private static final String NODE_PLAN_EXECUTOR = "plan-executor";
    private static final String NODE_ANALYST = "analyst";

    @GraphNode(id = NODE_PRIVILEGE_CHECKER, isStart = true, description = "Check if the user has privilege to perform log analysis")
    PrivilegeCheckNode privilegeCheckNode;

    @ConditionalEdge(source = NODE_PRIVILEGE_CHECKER, mappings = {"pass", NODE_EXECUTION_PLANNER, "interrupt", StateGraph.END})
    PrivilegeCheckDispatcher privilegeCheckRouting;

    @GraphNode(id = NODE_EXECUTION_PLANNER, next = NODE_PLAN_EXECUTOR, description = "Plan the execution steps for log analysis")
    ExecutionPlannerNode executionPlanner;

    @GraphNode(id = NODE_PLAN_EXECUTOR, next = NODE_ANALYST, description = "Follow the instruction to read and digest the log data")
    PlanExecutorNode planExecutor;

    @GraphNode(id = NODE_ANALYST, description = "Analyze the execution transcript and generate a report, loop back if context is insufficient")
    LogAnalystNode analyst;

    @ConditionalEdge(source = NODE_ANALYST, mappings = {"end", StateGraph.END, "loop", NODE_PLAN_EXECUTOR})
    LoopAnalystDispatcher needLoopCondition;

    @GraphCompileConfig
    final CompileConfig config = CompileConfig.builder()
                .saverConfig(SaverConfig.builder()
                        .register(saver)
                        .build())
                .build();

    public record ExecutionRecord(
            @JacksonXmlProperty(localName = "action") String action,
            @JacksonXmlProperty(localName = "result") String result
    ) {}

    @JacksonXmlRootElement(localName = "transcript")
    public record TranscriptWrapper(
            @JacksonXmlElementWrapper(useWrapping = false)
            @JacksonXmlProperty(localName = "step")
            List<ExecutionRecord> records,
            @JacksonXmlProperty(isAttribute = true)
            int interactionCount
    ) {}

}

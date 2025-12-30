package cn.teacy.wdd.agent.graph.service;

import cn.teacy.wdd.agent.graph.LogAnalyseGraphComposer;
import cn.teacy.wdd.agent.utils.GraphUtils;
import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

@Service
public class LogAnalyseGraphService {

    private final CompiledGraph logAnalyseGraph;

    public LogAnalyseGraphService(
            @Qualifier("logAnalyseGraph") CompiledGraph logAnalyseGraph
    ) {
        this.logAnalyseGraph = logAnalyseGraph;
    }

    public record ExecResult(
            GraphUtils.GraphExecResult result,
            RunnableConfig config
    ) {}

    public ExecResult execute(String probeId, String query) {

        Map<String, Object> initial = Map.of(LogAnalyseGraphComposer.KEY_QUERY, query);

        RunnableConfig config = RunnableConfig.builder()
                .addMetadata("probe_id", probeId)
                .threadId(UUID.randomUUID().toString())
                .build();

        return new ExecResult(
                GraphUtils.executeUntilInterrupt(logAnalyseGraph, initial, config),
                config
        );
    }

    public ExecResult continueExec(RunnableConfig config) {
        return new ExecResult(
                GraphUtils.executeUntilInterrupt(logAnalyseGraph, Map.of(), config),
                config
        );
    }

}

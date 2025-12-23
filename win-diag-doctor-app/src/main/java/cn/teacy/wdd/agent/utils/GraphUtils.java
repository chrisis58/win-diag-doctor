package cn.teacy.wdd.agent.utils;

import cn.teacy.wdd.agent.common.GraphKeys;
import com.alibaba.cloud.ai.graph.*;
import com.alibaba.cloud.ai.graph.action.InterruptionMetadata;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class GraphUtils {

    public static KeyStrategyFactory buildKeyStrategyFactory(Class<? extends GraphKeys> clazz) {
        if (clazz == null || !clazz.isEnum()) {
            throw new IllegalArgumentException("Class must be an enum type");
        }

        var enumConstants = clazz.getEnumConstants();
        var strategyMap = java.util.Arrays.stream(enumConstants)
                .collect(java.util.stream.Collectors.toUnmodifiableMap(
                        GraphKeys::getKey,
                        GraphKeys::getStrategy,
                        (existing, replacement) -> existing
                ));

        return () -> strategyMap;
    }

    public record GraphExecResult(
            OverAllState state,
            String nodeId,
            boolean interrupted
    ) {}

    /**
     * @see <a href="https://github.com/alibaba/spring-ai-alibaba/blob/main/examples/documentation/src/main/java/com/alibaba/cloud/ai/examples/documentation/graph/examples/HumanInTheLoopExample.java#L118">Official Implementation</a>
     */
    @NonNull
    public static GraphExecResult executeUntilInterrupt(CompiledGraph graph, Map<String, Object> input, RunnableConfig config) {

        // 用于保存最后一个输出
        AtomicReference<NodeOutput> lastOutputRef = new AtomicReference<>();

        // 运行 Graph 直到第一个中断点
        graph.stream(input, config)
                .doOnNext(nodeOutput -> {
                    log.debug("Node output: {}", nodeOutput);
                    lastOutputRef.set(nodeOutput);
                })
                .doOnError(error -> log.error("Graph execute error: {}", error.getMessage()))
                .doOnComplete(() -> log.debug("Graph execution completed"))
                .blockLast();

        // 检查最后一个输出是否是 InterruptionMetadata
        NodeOutput lastOutput = lastOutputRef.get();
        if (lastOutput == null) {
            throw new RuntimeException("Graph execution failed: output is null");
        }

        boolean interrupted = false;
        if (lastOutput instanceof InterruptionMetadata metadata) {
            log.info("Detected interruption: {}", metadata);
            interrupted = true;
        }

        return new GraphExecResult(lastOutput.state(), lastOutput.node(), interrupted);
    }

}

package cn.teacy.wdd.agent.graph.dispatcher;

import cn.teacy.wdd.agent.graph.LogAnalyseGraphComposer;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.EdgeAction;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class LoopAnalystDispatcher implements EdgeAction {

    private static final String KEY_EXECUTOR_INSTRUCTION = LogAnalyseGraphComposer.KEY_EXECUTOR_INSTRUCTION;
    private static final String KEY_ITERATION_COUNT = LogAnalyseGraphComposer.KEY_ITERATION_COUNT;

    @Override
    public String apply(OverAllState state) {
        Optional<Object> instruction = state.value(KEY_EXECUTOR_INSTRUCTION);

        int count = (int) state.value(KEY_ITERATION_COUNT).orElseThrow();
        if (count >= 3) {
            return "end";
        }

        return instruction.isPresent() && !instruction.get().toString().isBlank() ? "loop" : "end";
    }
}

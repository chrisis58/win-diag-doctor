package cn.teacy.wdd.agent.graph.dispatcher;

import cn.teacy.wdd.agent.graph.LogAnalyseGraphComposer;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.action.Command;
import com.alibaba.cloud.ai.graph.action.CommandAction;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

@Component
public class LoopAnalystDispatcher implements CommandAction {

    private static final String KEY_EXECUTOR_INSTRUCTION = LogAnalyseGraphComposer.KEY_EXECUTOR_INSTRUCTION;
    private static final String KEY_ITERATION_COUNT = LogAnalyseGraphComposer.KEY_ITERATION_COUNT;

    @Override
    public Command apply(OverAllState state, RunnableConfig config) {

        if (shouldLoop(state)) {
            return new Command("loop");
        } else {
            return new Command(
                    "end",
                    Map.of(KEY_ITERATION_COUNT, 0)
            );
        }
    }

    private boolean shouldLoop(OverAllState state) {
        Optional<Object> instruction = state.value(KEY_EXECUTOR_INSTRUCTION);

        int count = (int) state.value(KEY_ITERATION_COUNT).orElseThrow();
        if (count >= 3) {
            return false;
        }

        return instruction.isPresent() && !instruction.get().toString().isBlank();
    }
}

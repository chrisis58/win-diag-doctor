package cn.teacy.wdd.agent.graph.dispatcher;

import cn.teacy.wdd.agent.graph.LogAnalyseGraphComposer;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.EdgeAction;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class PrivilegeCheckDispatcher implements EdgeAction {

    private static final String KEY_PRIVILEGE_QUALIFIED = LogAnalyseGraphComposer.KEY_PRIVILEGE_QUALIFIED;

    @Override
    public String apply(OverAllState state) {
        Optional<Object> value = state.value(KEY_PRIVILEGE_QUALIFIED);

        if (value.isPresent() && String.valueOf(Boolean.TRUE).equals(value.get())) {
            // Privilege check passed
            return "pass";
        }
        return "interrupt";
    }
}

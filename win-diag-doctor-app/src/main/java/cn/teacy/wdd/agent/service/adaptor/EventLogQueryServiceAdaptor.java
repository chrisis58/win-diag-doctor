package cn.teacy.wdd.agent.service.adaptor;

import cn.teacy.wdd.agent.service.IEventLogQueryService;
import cn.teacy.wdd.protocol.command.LogQueryRequest;
import cn.teacy.wdd.protocol.response.LogQueryResponse;
import cn.teacy.wdd.service.LogQueryService;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;

import static com.alibaba.cloud.ai.graph.agent.tools.ToolContextConstants.AGENT_CONFIG_CONTEXT_KEY;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventLogQueryServiceAdaptor implements IEventLogQueryService {

    private final LogQueryService logQueryService;

    @Override
    public LogQueryResponse queryEventLogs(LogQueryRequest request, ToolContext context) {
        Optional<Map<String, Object>> metadata = ((RunnableConfig) context.getContext().get(AGENT_CONFIG_CONTEXT_KEY)).metadata();

        if (metadata.isEmpty()) {
            log.warn("cannot get probe metadata");
            return null;
        }

        String probeId = metadata.get().get("probe_id").toString();

        if (probeId == null || probeId.isBlank()) {
            log.warn("probeId is null or empty");
            return null;
        }

        try {
            return logQueryService.queryLog(probeId, request);
        } catch (Exception e) {
            return null;
        }
    }
}

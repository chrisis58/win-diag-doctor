package cn.teacy.wdd.service;

import cn.teacy.wdd.agent.common.ProbeIdMissingException;
import cn.teacy.wdd.agent.service.IUserContextProvider;
import cn.teacy.wdd.common.entity.UserContext;
import cn.teacy.wdd.protocol.WsMessagePayload;
import cn.teacy.wdd.protocol.command.GetUserContext;
import cn.teacy.wdd.protocol.response.GetUserContextResponse;
import cn.teacy.wdd.protocol.response.LogQueryResponse;
import cn.teacy.wdd.support.TaskHandle;
import cn.teacy.wdd.protocol.WsMessage;
import cn.teacy.wdd.protocol.command.LogQueryRequest;
import cn.teacy.wdd.websocket.WsSessionManager;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.utils.TypeRef;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Slf4j
@Service
@RequiredArgsConstructor
public class LogQueryService implements IUserContextProvider {

    private static final long ACK_TIMEOUT_SECONDS = 5;
    private static final long QUERY_TIMEOUT_SECONDS = 60;

    private final IPendingTaskRegistry<LogQueryResponse> logQueryTaskRegistry;
    private final IPendingTaskRegistry<GetUserContextResponse> userContextTaskRegistry;
    private final WsSessionManager sessionManager;

    @Override
    public UserContext getUserContext(OverAllState state, RunnableConfig config) {
        String probeId = config.metadata("probe_id", new TypeRef<String>() {
        }).orElseThrow(() -> new ProbeIdMissingException("Probe ID missing in context."));

        return this.getUserContext(probeId).getUserContext();
    }

    public GetUserContextResponse getUserContext(String probeId) {
        return executeSyncCommand(
                probeId,
                new GetUserContext(),
                userContextTaskRegistry,
                "获取用户上下文"
        );
    }

    public LogQueryResponse queryLog(String probeId, LogQueryRequest queryRequest) {
        return executeSyncCommand(
                probeId,
                queryRequest,
                logQueryTaskRegistry,
                "日志查询"
        );
    }

    private <P extends WsMessagePayload, R> R executeSyncCommand(
            String probeId,
            P payload,
            IPendingTaskRegistry<R> registry,
            String operationName
    ) {
        WsMessage<P> wsMessage = new WsMessage<>(payload);
        String taskId = wsMessage.getTaskId();

        TaskHandle<R> handle = registry.register(taskId);

        try {
            sessionManager.send(probeId, wsMessage);
            log.debug("[{}] 指令已发送，TaskId={}, 等待探针响应...", operationName, taskId);

            handle.getAckFuture().get(ACK_TIMEOUT_SECONDS, TimeUnit.SECONDS);

            return handle.getResultFuture().get(QUERY_TIMEOUT_SECONDS, TimeUnit.SECONDS);

        } catch (TimeoutException e) {
            log.warn("[{}] 超时: probeId={}, taskId={}", operationName, probeId, taskId);
            registry.fail(taskId, new RuntimeException(operationName + " Timeout"));
            throw new RuntimeException(operationName + "超时，探针未响应");

        } catch (InterruptedException e) {
            log.warn("[{}] 线程被中断: probeId={}, taskId={}", operationName, probeId, taskId);
            registry.fail(taskId, e);
            Thread.currentThread().interrupt();
            throw new RuntimeException(operationName + "被中断");

        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            log.error("[{}] 执行异常: probeId={}, taskId={}", operationName, probeId, taskId, cause);
            throw new RuntimeException(operationName + "失败: " + (cause != null ? cause.getMessage() : e.getMessage()));

        } catch (Exception e) {
            log.error("[{}] 发送失败: probeId={}, taskId={}", operationName, probeId, taskId, e);
            registry.fail(taskId, e);
            throw new RuntimeException("无法向探针发送[" + operationName + "]指令: " + e.getMessage());
        }
    }

}

package cn.teacy.wdd.service;

import cn.teacy.wdd.common.entity.WinEventLogEntry;
import cn.teacy.wdd.common.support.TaskHandle;
import cn.teacy.wdd.protocol.WsMessage;
import cn.teacy.wdd.protocol.command.LogQueryRequest;
import cn.teacy.wdd.websocket.WsSessionManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Slf4j
@Service
@RequiredArgsConstructor
public class LogQueryService {

    private static final long ACK_TIMEOUT_SECONDS = 5;
    private static final long QUERY_TIMEOUT_SECONDS = 60;

    private final IPendingTaskRegistry<List<WinEventLogEntry>> pendingTaskRegistry;
    private final WsSessionManager sessionManager;


    public List<WinEventLogEntry> queryLog(String probeId, LogQueryRequest queryRequest) {

        WsMessage<LogQueryRequest> wsMessage = new WsMessage<>(queryRequest);
        String taskId = wsMessage.getTaskId();

        TaskHandle<List<WinEventLogEntry>> handle = pendingTaskRegistry.register(taskId);

        try {
            sessionManager.send(probeId, wsMessage);
            log.debug("指令已发送，正在等待探针响应...");

        } catch (Exception e) {
            log.error("发送指令失败: taskId={}", taskId, e);
            pendingTaskRegistry.fail(taskId, e);
            throw new RuntimeException("无法向探针发送查询指令: " + e.getMessage());
        }

        try {
            handle.getAckFuture().get(ACK_TIMEOUT_SECONDS, TimeUnit.SECONDS);

            return handle.getResultFuture().get(QUERY_TIMEOUT_SECONDS, TimeUnit.SECONDS);

        } catch (TimeoutException e) {
            log.warn("日志查询超时: taskId={}", taskId);
            pendingTaskRegistry.fail(taskId, new RuntimeException("Query Timed Out"));
            throw new RuntimeException("查询超时，探针未响应");

        } catch (InterruptedException e) {
            log.warn("查询线程被中断: taskId={}", taskId);
            Thread.currentThread().interrupt(); // 恢复中断状态
            throw new RuntimeException("查询被中断");

        } catch (ExecutionException e) {
            log.error("任务执行期间发生异常: taskId={}", taskId, e.getCause());
            throw new RuntimeException("查询失败: " + e.getCause().getMessage());
        }
    }

}

package cn.teacy.wdd.service;

import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public interface IPendingTaskRegistry<R> {

    CompletableFuture<R> register(String taskId);

    void complete(String taskId, R result);

    void fail(String taskId, Throwable ex);

    @Slf4j
    class BasePendingTaskRegistry<P> implements IPendingTaskRegistry<P> {

        private final Map<String, CompletableFuture<P>> pendingTasks = new ConcurrentHashMap<>();

        @Override
        public CompletableFuture<P> register(String taskId) {
            CompletableFuture<P> future = new CompletableFuture<>();
            pendingTasks.put(taskId, future);
            future.whenComplete((res, ex) -> pendingTasks.remove(taskId));
            return future;
        }

        @Override
        public void complete(String taskId, P result) {
            CompletableFuture<P> future = pendingTasks.get(taskId);
            if (future != null) {
                log.info("收到任务数据，正在唤醒等待线程: taskId={}, result={}", taskId, result);
                future.complete(result);
            } else {
                log.warn("收到数据但未找到对应任务 (可能已超时): taskId={}", taskId);
            }
        }

        @Override
        public void fail(String taskId, Throwable ex) {
            CompletableFuture<P> future = pendingTasks.get(taskId);
            if (future != null) {
                future.completeExceptionally(ex);
            }
        }
    }

}

package cn.teacy.wdd.service;

import cn.teacy.wdd.common.support.TaskHandle;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public interface IPendingTaskRegistry<R> {

    TaskHandle<R> register(String taskId);

    void ack(String taskId);

    void complete(String taskId, R result);

    void fail(String taskId, Throwable ex);

    boolean hasTask(String taskId);

    @Slf4j
    class BasePendingTaskRegistry<P> implements IPendingTaskRegistry<P> {

        private final Map<String, TaskHandle<P>> pendingTasks = new ConcurrentHashMap<>();

        @Override
        public TaskHandle<P> register(String taskId) {
            TaskHandle<P> handle = new TaskHandle<>();
            pendingTasks.put(taskId, handle);
            handle.getResultFuture().whenComplete((res, ex) -> pendingTasks.remove(taskId));
            return handle;
        }

        @Override
        public void ack(String taskId) {
            TaskHandle<P> handle = pendingTasks.get(taskId);
            if (handle != null) {
                log.info("收到任务确认，正在唤醒等待线程: taskId={}", taskId);
                handle.getAckFuture().complete(null);
            } else {
                log.warn("收到确认但未找到对应任务: taskId={}", taskId);
            }
        }

        @Override
        public void complete(String taskId, P result) {
            TaskHandle<P> handle = pendingTasks.get(taskId);
            if (handle != null) {
                log.info("收到任务数据，正在唤醒等待线程: taskId={}, result={}", taskId, result);

                if (!handle.getAckFuture().isDone()) {
                    handle.getAckFuture().complete(null);
                }

                handle.getResultFuture().complete(result);
            } else {
                log.warn("收到数据但未找到对应任务 (可能已超时): taskId={}", taskId);
            }
        }

        @Override
        public void fail(String taskId, Throwable ex) {
            TaskHandle<P> handle = pendingTasks.get(taskId);
            if (handle != null) {
                handle.getAckFuture().completeExceptionally(ex);
                handle.getResultFuture().completeExceptionally(ex);
            }
        }

        @Override
        public boolean hasTask(String taskId) {
            return pendingTasks.containsKey(taskId);
        }
    }

}

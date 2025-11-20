package cn.teacy.wdd.common.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TaskExecutionResult<T> {

    private String taskId;

    private boolean success;

    private String message;

    private T data;

    public static <T> TaskExecutionResult<T> success(String taskId, T data) {
        return new TaskExecutionResult<>(taskId, true, "Success", data);
    }

    public static <T> TaskExecutionResult<T> failure(String taskId, String message) {
        return new TaskExecutionResult<>(taskId, false, message, null);
    }

}

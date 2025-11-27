package cn.teacy.wdd.protocol.event;

import cn.teacy.wdd.protocol.WsMessagePayload;
import cn.teacy.wdd.protocol.WsProtocol;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import lombok.*;

import java.util.HashMap;
import java.util.Map;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@WsProtocol(identifier = "task:status:update")
public class TaskStatusUpdate extends WsMessagePayload {

    private String taskId;

    private String message;

    private TaskState state;

    @Builder.Default
    private Map<String, Object> extra = new HashMap<>();

    @JsonAnyGetter
    public Map<String, Object> getExtra() {
        return extra;
    }

    @JsonAnySetter
    public void addExtra(String key, Object value) {
        this.extra.put(key, value);
    }

    public static TaskStatusUpdate ack(String taskId) {
        return TaskStatusUpdate.builder()
                .taskId(taskId)
                .state(TaskState.RECEIVED)
                .build();
    }

    public static TaskStatusUpdate fail(String taskId, String message) {
        return TaskStatusUpdate.builder()
                .taskId(taskId)
                .state(TaskState.FAILED)
                .message(message)
                .build();
    }

    public enum TaskState {

        RECEIVED,

        RUNNING,

        UPLOADING,

        COMPLETED,

        FAILED
    }
}

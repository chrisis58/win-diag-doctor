package cn.teacy.wdd.controller;

import cn.teacy.wdd.common.entity.TaskExecutionResult;
import cn.teacy.wdd.common.entity.WinEventLogEntry;
import cn.teacy.wdd.service.IPendingTaskRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/tasks")
public class TaskResultController {

    private final IPendingTaskRegistry<List<WinEventLogEntry>> pendingTaskRegistry;

    @PostMapping("/log-result")
    public ResponseEntity<Void> acceptLogQueryResult(@RequestBody TaskExecutionResult<List<WinEventLogEntry>> result) {

        if (result.isSuccess()) {
            pendingTaskRegistry.complete(result.getTaskId(), result.getData());
        } else {
            pendingTaskRegistry.fail(result.getTaskId(), new RuntimeException(result.getMessage()));
        }
        return ResponseEntity.ok().build();
    }

}

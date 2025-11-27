package cn.teacy.wdd.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class TaskRegistryRouter {

    private final List<IPendingTaskRegistry<?>> taskRegistries;

    public Optional<IPendingTaskRegistry<?>> getTaskRegistry(String taskId) {
        return taskRegistries.stream()
                .filter(registry -> registry.hasTask(taskId))
                .findFirst();
    }

}

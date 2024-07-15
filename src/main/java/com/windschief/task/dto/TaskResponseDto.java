package com.windschief.task.dto;

import java.time.Instant;

import com.windschief.task.domain.Platform;
import com.windschief.task.domain.Task;

public record TaskResponseDto(
        Long id,
        Platform platform,
        String executionInterval,
        Instant lastTimeExecuted,
        Instant checkFrom,
        boolean active) {
    public static TaskResponseDto from(Task task) {
        if (task == null) {
            throw new IllegalArgumentException("Task cannot be null");
        }
        return new TaskResponseDto(
                task.getId(),
                task.getPlatform(),
                task.getExecutionInterval(),
                task.getLastTimeExecuted(),
                task.getCheckFrom(),
                task.isActive()
        );
    }
}

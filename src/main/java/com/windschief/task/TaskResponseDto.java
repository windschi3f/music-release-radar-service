package com.windschief.task;

import java.time.Instant;

public record TaskResponseDto(
        Long id,
        Platform platform,
        int executionIntervalDays,
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
                task.getExecutionIntervalDays(),
                task.getLastTimeExecuted(),
                task.getCheckFrom(),
                task.isActive());
    }
}

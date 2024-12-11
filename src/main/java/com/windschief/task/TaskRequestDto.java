package com.windschief.task;

import java.time.Instant;

public record TaskRequestDto(
        Platform platform,
        int executionIntervalDays,
        Instant checkFrom,
        boolean active) {
    public static Task toTask(TaskRequestDto taskRequestDto) {
        if (taskRequestDto == null) {
            throw new IllegalArgumentException("TaskRequestDto cannot be null");
        }
        Task task = new Task();
        task.setPlatform(taskRequestDto.platform());
        task.setExecutionIntervalDays(taskRequestDto.executionIntervalDays());
        task.setCheckFrom(taskRequestDto.checkFrom());
        task.setActive(taskRequestDto.active());
        return task;
    }
}

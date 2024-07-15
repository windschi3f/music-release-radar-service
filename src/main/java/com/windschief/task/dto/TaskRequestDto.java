package com.windschief.task.dto;

import java.time.Instant;

import com.windschief.task.domain.Platform;
import com.windschief.task.domain.Task;

public record TaskRequestDto(
                Platform platform,
                String executionInterval,
                Instant checkFrom,
                boolean active) {
    public static Task toTask(TaskRequestDto taskRequestDto) {
        if (taskRequestDto == null) {
            throw new IllegalArgumentException("TaskRequestDto cannot be null");
        }
        Task task = new Task();
        task.setPlatform(taskRequestDto.platform());
        task.setExecutionInterval(taskRequestDto.executionInterval());
        task.setCheckFrom(taskRequestDto.checkFrom());
        task.setActive(taskRequestDto.active());
        return task;
    }
}

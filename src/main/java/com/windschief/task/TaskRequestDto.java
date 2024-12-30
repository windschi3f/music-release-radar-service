package com.windschief.task;

import java.time.Instant;

public record TaskRequestDto(
        Platform platform,
        int executionIntervalDays,
        Instant checkFrom,
        boolean active,
        String externalDestinationId,
        String refreshToken) {
    public static Task toTask(TaskRequestDto taskRequestDto) {
        if (taskRequestDto == null) {
            throw new IllegalArgumentException("TaskRequestDto cannot be null");
        }
        Task task = new Task();
        task.setPlatform(taskRequestDto.platform());
        task.setExecutionIntervalDays(taskRequestDto.executionIntervalDays());
        task.setCheckFrom(taskRequestDto.checkFrom());
        task.setActive(taskRequestDto.active());
        task.setExternalDestinationId(taskRequestDto.externalDestinationId());
        return task;
    }

    public static void updateTask(Task task, TaskRequestDto dto) {
        if (dto == null || task == null) {
            throw new IllegalArgumentException("Task and TaskRequestDto cannot be null");
        }
        task.setPlatform(dto.platform());
        task.setExecutionIntervalDays(dto.executionIntervalDays());
        task.setCheckFrom(dto.checkFrom());
        task.setActive(dto.active());
        task.setExternalDestinationId(dto.externalDestinationId());
    }
}

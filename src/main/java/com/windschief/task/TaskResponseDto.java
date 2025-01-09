package com.windschief.task;

import java.time.Instant;

public record TaskResponseDto(
        Long id,
        String name,
        Platform platform,
        int executionIntervalDays,
        Instant lastTimeExecuted,
        Instant checkFrom,
        boolean active,
        String playlistId) {
    public static TaskResponseDto from(Task task) {
        if (task == null) {
            throw new IllegalArgumentException("Task cannot be null");
        }
        return new TaskResponseDto(
                task.getId(),
                task.getName(),
                task.getPlatform(),
                task.getExecutionIntervalDays(),
                task.getLastTimeExecuted(),
                task.getCheckFrom(),
                task.isActive(),
                task.getPlaylistId());
    }
}

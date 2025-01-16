package com.windschief.task;

import java.time.LocalDate;

public record TaskRequestDto(
        String name,
        Platform platform,
        int executionIntervalDays,
        LocalDate checkFrom,
        boolean active,
        String playlistId) {
    public static Task toTask(TaskRequestDto taskRequestDto) {
        if (taskRequestDto == null) {
            throw new IllegalArgumentException("TaskRequestDto cannot be null");
        }
        Task task = new Task();
        task.setName(taskRequestDto.name());
        task.setPlatform(taskRequestDto.platform());
        task.setExecutionIntervalDays(taskRequestDto.executionIntervalDays());
        task.setCheckFrom(taskRequestDto.checkFrom());
        task.setActive(taskRequestDto.active());
        task.setPlaylistId(taskRequestDto.playlistId());
        return task;
    }

    public static void updateTask(Task task, TaskRequestDto dto) {
        if (dto == null || task == null) {
            throw new IllegalArgumentException("Task and TaskRequestDto cannot be null");
        }
        task.setName(dto.name());
        task.setPlatform(dto.platform());
        task.setExecutionIntervalDays(dto.executionIntervalDays());
        task.setCheckFrom(dto.checkFrom());
        task.setActive(dto.active());
        task.setPlaylistId(dto.playlistId());
    }
}

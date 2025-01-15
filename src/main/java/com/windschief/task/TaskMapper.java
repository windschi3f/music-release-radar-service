package com.windschief.task;

import com.windschief.releasedetection.ReleaseRadarService;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class TaskMapper {
    private final ReleaseRadarService releaseRadarService;

    @Inject
    public TaskMapper(ReleaseRadarService releaseRadarService) {
        this.releaseRadarService = releaseRadarService;
    }

    public TaskResponseDto toDto(Task task) {
        return new TaskResponseDto(
            task.getId(),
            task.getName(),
            task.getPlatform(),
            task.getExecutionIntervalDays(),
            task.getLastTimeExecuted(),
            task.getCheckFrom(),
            task.isActive(),
            task.getPlaylistId(),
            releaseRadarService.isTaskProcessing(task.getId()));
    }
}

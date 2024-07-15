package com.windschief.task.dto;

import com.windschief.task.domain.TaskItem;
import com.windschief.task.domain.TaskItemType;

public record TaskItemResponseDto(
        Long id,
        TaskItemType itemType,
        String externalReferenceId) {
    public static TaskItemResponseDto from(TaskItem taskItem) {
        if (taskItem == null) {
            throw new IllegalArgumentException("Task cannot be null");
        }
        return new TaskItemResponseDto(
                taskItem.getId(),
                taskItem.getItemType(),
                taskItem.getExternalReferenceId()
        );
    }
}

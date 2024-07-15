package com.windschief.task.dto;

import com.windschief.task.domain.TaskItem;
import com.windschief.task.domain.TaskItemType;

public record TaskItemRequestDto(
        TaskItemType itemType,
        String externalReferenceId) {
    public static TaskItem toTaskItem(TaskItemRequestDto taskItemRequestDto) {
        if (taskItemRequestDto == null) {
            throw new IllegalArgumentException("TaskItemResponseDto cannot be null");
        }
        TaskItem taskItem = new TaskItem();
        taskItem.setItemType(taskItemRequestDto.itemType());
        taskItem.setExternalReferenceId(taskItemRequestDto.externalReferenceId());
        return taskItem;
    }
}

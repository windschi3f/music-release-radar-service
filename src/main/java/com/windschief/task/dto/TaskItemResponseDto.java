package com.windschief.task.dto;

import com.windschief.task.model.TaskItemType;

public record TaskItemResponseDto(
        Long id,
        Long taskId,
        TaskItemType itemType,
        String itemId) {
}

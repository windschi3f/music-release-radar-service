package com.windschief.task.dto;

import com.windschief.task.model.TaskItemType;

public record TaskItemRequestDto(
                Long taskId,
                TaskItemType itemType,
                String itemId) {
}

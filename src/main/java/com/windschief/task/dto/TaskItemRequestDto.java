package com.windschief.task.dto;

import com.windschief.task.domain.TaskItemType;

public record TaskItemRequestDto(
                Long taskId,
                TaskItemType itemType,
                String itemId) {
}

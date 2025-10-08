package com.windschief.task.item;

public record TaskItemResponseDto(
        Long id,
        TaskItemType itemType,
        String externalId) {
    public static TaskItemResponseDto from(TaskItem taskItem) {
        if (taskItem == null) {
            throw new IllegalArgumentException("Task cannot be null");
        }
        return new TaskItemResponseDto(
                taskItem.getId(),
                taskItem.getItemType(),
                taskItem.getExternalId());
    }
}

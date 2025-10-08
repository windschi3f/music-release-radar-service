package com.windschief.task.item;

public record TaskItemRequestDto(
        TaskItemType itemType,
        String externalId) {
    public static TaskItem toTaskItem(TaskItemRequestDto taskItemRequestDto) {
        if (taskItemRequestDto == null) {
            throw new IllegalArgumentException("TaskItemResponseDto cannot be null");
        }
        TaskItem taskItem = new TaskItem();
        taskItem.setItemType(taskItemRequestDto.itemType());
        taskItem.setExternalId(taskItemRequestDto.externalId());
        return taskItem;
    }
}

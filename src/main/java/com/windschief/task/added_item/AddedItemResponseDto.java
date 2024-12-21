package com.windschief.task.added_item;

import java.time.Instant;

public record AddedItemResponseDto(
        Long id,
        String externalId,
        String title,
        Instant addedAt) {
    public static AddedItemResponseDto from(AddedItem addedItem) {
        return new AddedItemResponseDto(
                addedItem.getId(),
                addedItem.getExternalId(),
                addedItem.getTitle(),
                addedItem.getAddedAt());
    }
}
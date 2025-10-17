package com.windschief.user;

import java.util.List;

import com.windschief.auth.SpotifyToken;
import com.windschief.task.TaskResponseDto;
import com.windschief.task.added_item.AddedItem;
import com.windschief.task.item.TaskItemResponseDto;

public record UserDataDto(
    String userId,
    SpotifyToken spotifyToken,
    List<TaskResponseDto> tasks,
    List<TaskItemResponseDto> taskItems,
    List<AddedItem> addedItems
) {
}

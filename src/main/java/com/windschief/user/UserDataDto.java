package com.windschief.user;

import java.util.List;

import com.windschief.auth.SpotifyToken;
import com.windschief.task.Task;
import com.windschief.task.added_item.AddedItem;

public record UserDataDto(
    String userId,
    SpotifyToken spotifyToken,
    List<Task> tasks,
    List<AddedItem> addedItems
) {
}

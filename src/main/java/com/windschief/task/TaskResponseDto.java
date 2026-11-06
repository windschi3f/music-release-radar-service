package com.windschief.task;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record TaskResponseDto(
        Long id,
        String name,
        Platform platform,
        int executionIntervalDays,
        Instant lastTimeExecuted,
        LocalDate checkFrom,
        boolean active,
        String playlistId,
        boolean processing,
        String favoritePlaylistId,
        TasteSource tasteSource,
        int discoveryLimit,
        boolean replacePlaylist,
        String lastRunMessage,
        boolean lastRunFailed,
        List<String> sourcePlaylistIds,
        int recentDays) {
}

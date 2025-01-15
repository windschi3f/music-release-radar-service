package com.windschief.task;

import java.time.Instant;

public record TaskResponseDto(
        Long id,
        String name,
        Platform platform,
        int executionIntervalDays,
        Instant lastTimeExecuted,
        Instant checkFrom,
        boolean active,
        String playlistId,
        boolean processing) {
}

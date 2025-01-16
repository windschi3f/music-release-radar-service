package com.windschief.task;

import java.time.Instant;
import java.time.LocalDate;

public record TaskResponseDto(
        Long id,
        String name,
        Platform platform,
        int executionIntervalDays,
        Instant lastTimeExecuted,
        LocalDate checkFrom,
        boolean active,
        String playlistId,
        boolean processing) {
}

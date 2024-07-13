package com.windschief.task.dto;

import java.time.Instant;

import com.windschief.task.domain.Platform;

public record TaskResponseDto(
        Long id,
        Platform platform,
        String executionInterval,
        Instant lastTimeExecuted,
        Instant checkFrom,
        boolean active,
        String userId) {
}

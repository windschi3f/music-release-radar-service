package com.windschief.task.dto;

import java.time.Instant;

import com.windschief.task.domain.Platform;

public record TaskRequestDto(
                Platform platform,
                String executionInterval,
                Instant checkFrom,
                boolean active,
                String userId) {
}

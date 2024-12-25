package com.windschief.auth;

import java.time.Instant;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public record SpotifyToken(
        @Id String userId,
        String accessToken,
        String refreshToken,
        Instant expiresAt) {

    public SpotifyToken withNewAccessToken(String newAccessToken, Instant newExpiresAt) {
        return new SpotifyToken(userId, newAccessToken, refreshToken, newExpiresAt);
    }
}
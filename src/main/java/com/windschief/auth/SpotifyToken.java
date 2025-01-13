package com.windschief.auth;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class SpotifyToken {
    @Id
    private String userId;
    @Column(length = 2048)
    private String accessToken;
    @Column(length = 2048)
    private String refreshToken;
    private Instant expiresAt;

    protected SpotifyToken() {
    }

    public SpotifyToken(String userId, String accessToken, String refreshToken, Instant expiresAt) {
        this.userId = userId;
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.expiresAt = expiresAt;
    }

    public SpotifyToken withNewAccessToken(String newAccessToken, Instant newExpiresAt) {
        return new SpotifyToken(userId, newAccessToken, refreshToken, newExpiresAt);
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

}
package com.windschief.spotify.model;

public record TokenResponse(
        String access_token,
        String token_type,
        int expires_in,
        String scope,
        String refresh_token) {
}

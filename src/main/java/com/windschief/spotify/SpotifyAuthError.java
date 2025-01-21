package com.windschief.spotify;

public record SpotifyAuthError(
        String error,
        String error_description) {
}

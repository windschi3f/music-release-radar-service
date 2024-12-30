package com.windschief.auth;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.eclipse.microprofile.rest.client.inject.RestClient;

import com.windschief.spotify.SpotifyApi;
import com.windschief.spotify.model.SpotifyUser;

@ApplicationScoped
public class SpotifyTokenValidator {

    private final SpotifyApi spotifyApi;

    @Inject
    public SpotifyTokenValidator(@RestClient SpotifyApi spotifyApi) {
        this.spotifyApi = spotifyApi;
    }

    public Uni<SpotifyUser> validateToken(String token) {
        return Uni.createFrom().item(() -> {
            try {
                return spotifyApi.getCurrentUser("Bearer " + token);
            } catch (Exception e) {
                return null;
            }
        }).runSubscriptionOn(Infrastructure.getDefaultWorkerPool());
    }
}
package com.windschief.auth;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;

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
            } catch (WebApplicationException e) {
                // Log the error if needed
                return null;
            }
        });
    }
}
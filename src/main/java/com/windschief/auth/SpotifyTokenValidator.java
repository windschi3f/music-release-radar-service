package com.windschief.auth;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.time.Instant;
import java.util.Base64;

import org.eclipse.microprofile.rest.client.inject.RestClient;

import com.windschief.spotify.SpotifyApi;
import com.windschief.spotify.model.SpotifyUser;
import com.windschief.spotify.model.TokenResponse;

@ApplicationScoped
public class SpotifyTokenValidator {

    private final SpotifyApi spotifyApi;
    private final SpotifyTokenRepository tokenRepository;
    private final SpotifyConfig spotifyConfig;

    @Inject
    public SpotifyTokenValidator(
            @RestClient SpotifyApi spotifyApi,
            SpotifyTokenRepository tokenRepository,
            SpotifyConfig spotifyConfig) {
        this.spotifyApi = spotifyApi;
        this.tokenRepository = tokenRepository;
        this.spotifyConfig = spotifyConfig;
    }

    public Uni<SpotifyUser> validateToken(String token, String refreshToken) {
        return Uni.createFrom().item(() -> {
            try {
                SpotifyUser user = spotifyApi.getCurrentUser("Bearer " + token);
                updateStoredToken(user.id(), token, refreshToken);
                return user;
            } catch (Exception e) {
                return null;
            }
        });
    }

    public String getValidTokenForUser(String userId) throws Exception {
        SpotifyToken token = tokenRepository.findByUserId(userId);
        if (token == null) {
            return null;
        }

        if (token.expiresAt().isBefore(Instant.now()) && token.refreshToken() != null) {
            refreshToken(token);
        }
        return token.accessToken();
    }

    private void updateStoredToken(String userId, String accessToken, String refreshToken) {
        SpotifyToken storedToken = tokenRepository.findByUserId(userId);
        SpotifyToken newToken = (storedToken != null)
                ? storedToken.withNewAccessToken(accessToken, Instant.now().plusSeconds(3000))
                : new SpotifyToken(userId, accessToken, refreshToken, Instant.now().plusSeconds(3000));
        tokenRepository.persist(newToken);
    }

    private void refreshToken(SpotifyToken token) throws Exception {
        String basicAuth = "Basic " + Base64.getEncoder()
                .encodeToString((spotifyConfig.clientId() + ":" + spotifyConfig.clientSecret()).getBytes());

        TokenResponse response = spotifyApi.refreshToken(
                basicAuth,
                "refresh_token",
                token.refreshToken());

        SpotifyToken newToken = token.withNewAccessToken(
                response.access_token(),
                Instant.now().plusSeconds(response.expires_in()));
        tokenRepository.persist(newToken);
    }
}
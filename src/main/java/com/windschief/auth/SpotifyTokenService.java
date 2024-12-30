package com.windschief.auth;

import java.time.Instant;
import java.util.Base64;

import org.eclipse.microprofile.rest.client.inject.RestClient;

import com.windschief.spotify.SpotifyApi;
import com.windschief.spotify.model.TokenResponse;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class SpotifyTokenService {
    private final SpotifyTokenRepository tokenRepository;
    private final SpotifyConfig spotifyConfig;
    private final SpotifyApi spotifyApi;

    @Inject
    public SpotifyTokenService(SpotifyTokenRepository tokenRepository, SpotifyConfig spotifyConfig,
            @RestClient SpotifyApi spotifyApi) {
        this.tokenRepository = tokenRepository;
        this.spotifyConfig = spotifyConfig;
        this.spotifyApi = spotifyApi;
    }

    @Transactional
    public String getValidToken(String userId) throws Exception {
        SpotifyToken token = tokenRepository.findByUserId(userId);
        if (token == null) {
            return null;
        }

        if (token.expiresAt().isBefore(Instant.now()) && token.refreshToken() != null) {
            refreshToken(token);
        }
        return token.accessToken();
    }

    @Transactional
    public void updateStoredToken(String userId, String accessToken, String refreshToken) {
        SpotifyToken storedToken = tokenRepository.findByUserId(userId);
        SpotifyToken newToken = (storedToken != null)
                ? storedToken.withNewAccessToken(accessToken, Instant.now().plusSeconds(3000))
                : new SpotifyToken(userId, accessToken, refreshToken, Instant.now().plusSeconds(3000));
        tokenRepository.persist(newToken);
    }

    @Transactional
    void refreshToken(SpotifyToken token) throws Exception {
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

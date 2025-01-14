package com.windschief.auth;

import java.time.Instant;
import java.util.Base64;

import org.eclipse.microprofile.rest.client.inject.RestClient;

import com.windschief.spotify.SpotifyAccountsApi;
import com.windschief.spotify.model.TokenResponse;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.WebApplicationException;

@ApplicationScoped
public class SpotifyTokenService {
    private final SpotifyTokenRepository tokenRepository;
    private final SpotifyConfig spotifyConfig;
    private final SpotifyAccountsApi spotifyAccountsApi;

    @Inject
    public SpotifyTokenService(SpotifyTokenRepository tokenRepository, SpotifyConfig spotifyConfig,
            @RestClient SpotifyAccountsApi spotifyAccountsApi) {
        this.tokenRepository = tokenRepository;
        this.spotifyConfig = spotifyConfig;
        this.spotifyAccountsApi = spotifyAccountsApi;
    }

    @Transactional
    public String getValidBearerAccessToken(String userId) throws WebApplicationException {
        SpotifyToken token = tokenRepository.findByUserId(userId);
        if (token == null) {
            return null;
        }

        if (token.getExpiresAt().isBefore(Instant.now()) && token.getRefreshToken() != null) {
            refreshToken(token);
        }
        return "Bearer " + token.getAccessToken();
    }

    @Transactional
    public void updateStoredToken(String userId, String accessToken, String refreshToken) {
        SpotifyToken storedToken = tokenRepository.findByUserId(userId);

        if (storedToken == null) {
            SpotifyToken newToken = new SpotifyToken(userId, accessToken, refreshToken,
                Instant.now().plusSeconds(3000));
            tokenRepository.persist(newToken);
        } else {
            if (!accessToken.equals(storedToken.getAccessToken())) {
                storedToken.setAccessToken(accessToken);
                storedToken.setExpiresAt(Instant.now().plusSeconds(3000));
            }
            if (!refreshToken.equals(storedToken.getRefreshToken())) {
                storedToken.setRefreshToken(refreshToken);
            }
        }
    }

    @Transactional
    void refreshToken(SpotifyToken token) throws WebApplicationException {
        String basicAuth = "Basic " + Base64.getEncoder()
                .encodeToString((spotifyConfig.clientId() + ":" + spotifyConfig.clientSecret()).getBytes());

        TokenResponse response = spotifyAccountsApi.refreshToken(
                basicAuth,
                "refresh_token",
                token.getRefreshToken(),
                spotifyConfig.clientId());

        SpotifyToken newToken = token.withNewAccessToken(
                response.access_token(),
                Instant.now().plusSeconds(response.expires_in()));
        tokenRepository.persist(newToken);
    }
}

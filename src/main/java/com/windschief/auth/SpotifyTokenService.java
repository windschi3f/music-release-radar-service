package com.windschief.auth;

import java.time.Instant;
import java.util.Base64;

import org.eclipse.microprofile.rest.client.inject.RestClient;

import com.windschief.spotify.SpotifyAccountsApi;
import com.windschief.spotify.model.TokenResponse;

import io.quarkus.security.identity.SecurityIdentity;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

@ApplicationScoped
public class SpotifyTokenService implements SpotifyTokenApi {
    private final SpotifyTokenRepository tokenRepository;
    private final SpotifyConfig spotifyConfig;
    private final SpotifyAccountsApi spotifyAccountsApi;
    private final SecurityIdentity securityIdentity;

    @Inject
    public SpotifyTokenService(SpotifyTokenRepository tokenRepository, SpotifyConfig spotifyConfig,
            @RestClient SpotifyAccountsApi spotifyAccountsApi, SecurityIdentity securityIdentity) {
        this.tokenRepository = tokenRepository;
        this.spotifyConfig = spotifyConfig;
        this.spotifyAccountsApi = spotifyAccountsApi;
        this.securityIdentity = securityIdentity;
    }

    @Override
    @Transactional
    public Response storeRefreshToken(String refreshToken) {
        final String userId = securityIdentity.getPrincipal().getName();
        final SpotifyToken storedToken = tokenRepository.findByUserId(userId);

        if (storedToken == null) {
            final SpotifyToken newToken = new SpotifyToken(userId, null, refreshToken, Instant.now());
            tokenRepository.persist(newToken);
            return Response.status(Response.Status.CREATED).build();
        }

        storedToken.setRefreshToken(refreshToken);
        return Response.ok().build();
    }

    @Transactional
    public String getValidBearerAccessToken(String userId) throws WebApplicationException, SpotifyTokenException {
        SpotifyToken token = tokenRepository.findByUserId(userId);
        if (token == null || token.getRefreshToken() == null) {
            throw new SpotifyTokenException("No refresh token found for user " + userId);
        }

        if (token.getExpiresAt().isBefore(Instant.now())) {
            final TokenResponse tokenResponse = refreshToken(token);
            final Instant expiresAt = Instant.now().plusSeconds(tokenResponse.expires_in() - 60);

            token.setAccessToken(tokenResponse.access_token());
            token.setExpiresAt(expiresAt);
        }

        return "Bearer " + token.getAccessToken();
    }

    public boolean hasRefreshToken(String userId) {
        final SpotifyToken token = tokenRepository.findByUserId(userId);
        return token != null && token.getRefreshToken() != null;
    }

    private TokenResponse refreshToken(SpotifyToken token) throws WebApplicationException {
        String basicAuth = "Basic " + Base64.getEncoder()
                .encodeToString((spotifyConfig.clientId() + ":" + spotifyConfig.clientSecret()).getBytes());

        return spotifyAccountsApi.refreshToken(
                basicAuth,
                "refresh_token",
                token.getRefreshToken(),
                spotifyConfig.clientId());
    }
}

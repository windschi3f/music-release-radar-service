package com.windschief.auth;

import java.time.Instant;
import java.util.Base64;
import java.util.Optional;

import org.eclipse.microprofile.rest.client.inject.RestClient;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.windschief.spotify.SpotifyAccountsApi;
import com.windschief.spotify.SpotifyAuthError;
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
    private final ObjectMapper objectMapper = new ObjectMapper();

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
        final Optional<SpotifyToken> storedToken = tokenRepository.findByUserId(userId);

        if (storedToken.isEmpty()) {
            final SpotifyToken newToken = new SpotifyToken(userId, null, refreshToken, Instant.now());
            tokenRepository.persist(newToken);
            return Response.status(Response.Status.CREATED).build();
        }

        storedToken.get().setRefreshToken(refreshToken);
        return Response.ok().build();
    }

    @Transactional
    public String getValidBearerAccessToken(String userId) throws WebApplicationException, SpotifyTokenException {
        Optional<SpotifyToken> token = tokenRepository.findByUserId(userId);
        if (token.isEmpty() || token.get().getRefreshToken() == null) {
            throw new SpotifyTokenException("No refresh token found for user " + userId);
        }

        if (token.get().getExpiresAt().isBefore(Instant.now())) {
            final TokenResponse tokenResponse = refreshToken(token.get());
            final Instant expiresAt = Instant.now().plusSeconds(tokenResponse.expires_in() - 60);

            token.get().setAccessToken(tokenResponse.access_token());
            token.get().setExpiresAt(expiresAt);
        }

        return "Bearer " + token.get().getAccessToken();
    }

    public boolean hasRefreshToken(String userId) {
        final Optional<SpotifyToken> token = tokenRepository.findByUserId(userId);
        return token.isPresent() && token.get().getRefreshToken() != null;
    }

    private TokenResponse refreshToken(SpotifyToken token) throws WebApplicationException {
        final String basicAuth = "Basic " + Base64.getEncoder()
                .encodeToString((spotifyConfig.clientId() + ":" + spotifyConfig.clientSecret()).getBytes());

        try {
            return spotifyAccountsApi.refreshToken(
                basicAuth,
                "refresh_token",
                token.getRefreshToken(),
                spotifyConfig.clientId());
        } catch (WebApplicationException e) {
            String errorBody = e.getResponse().readEntity(String.class);
            try {
                SpotifyAuthError authError = objectMapper.readValue(errorBody, SpotifyAuthError.class);
                throw new WebApplicationException(
                        String.format("Spotify refresh token error: %s - %s",
                                authError.error(),
                                authError.error_description()),
                        e.getResponse().getStatus());
            } catch (JsonProcessingException parseException) {
                throw new WebApplicationException("Spotify refresh token error: " + errorBody,
                        e.getResponse().getStatus());
            }
        }
    }
}

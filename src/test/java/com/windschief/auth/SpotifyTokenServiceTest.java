package com.windschief.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Base64;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.windschief.spotify.SpotifyAccountsApi;
import com.windschief.spotify.model.TokenResponse;

import jakarta.ws.rs.WebApplicationException;


public class SpotifyTokenServiceTest {
    private final SpotifyTokenRepository tokenRepository = mock(SpotifyTokenRepository.class);
    private final SpotifyConfig spotifyConfig = mock(SpotifyConfig.class);
    private final SpotifyAccountsApi spotifyAccountsApi = mock(SpotifyAccountsApi.class);
    private final SpotifyTokenService spotifyTokenService = new SpotifyTokenService(tokenRepository, spotifyConfig, spotifyAccountsApi);

    @BeforeEach
    void setup() {
        when(spotifyConfig.clientId()).thenReturn("test-client-id");
        when(spotifyConfig.clientSecret()).thenReturn("test-client-secret");
    }

    @Test
    void givenNoToken_whenGetValidBearerAccessToken_thenThrowException() {
        // given
        final String userId = "test-user-id";
        when(tokenRepository.findByUserId(userId)).thenReturn(null);

        // when / then
        assertThrows(SpotifyTokenException.class, () -> spotifyTokenService.getValidBearerAccessToken(userId));
    }

    @Test
    void givenNoRefreshToken_whenGetValidBearerAccessToken_thenThrowException() {
        // given
        final String userId = "test-user-id";
        final SpotifyToken token = new SpotifyToken(userId, null, null, null);
        when(tokenRepository.findByUserId(userId)).thenReturn(token);

        // when / then
        assertThrows(SpotifyTokenException.class, () -> spotifyTokenService.getValidBearerAccessToken(userId));
    }

    @Test
    void givenExpiredToken_whenGetValidBearerAccessToken_thenRefreshToken() throws WebApplicationException, SpotifyTokenException {
        // given
        final String userId = "test-user-id";
        final String newAccessToken = "new-access-token";
        final SpotifyToken token = new SpotifyToken(userId, null, "test-refresh-token", Instant.now().minusSeconds(1));
        when(tokenRepository.findByUserId(userId)).thenReturn(token);
        
        TokenResponse tokenResponse = new TokenResponse(newAccessToken, "", 3600, userId, "test-refresh-token");
        
        when(spotifyAccountsApi.refreshToken(
            "Basic " + Base64.getEncoder().encodeToString("test-client-id:test-client-secret".getBytes()),
            "refresh_token",
            "test-refresh-token",
            "test-client-id"
        )).thenReturn(tokenResponse);

        // when
        String result = spotifyTokenService.getValidBearerAccessToken(userId);

        // then
        assertEquals("Bearer " + newAccessToken, result);
        assertEquals(newAccessToken, token.getAccessToken());
        assertNotNull(token.getExpiresAt());
    }

    @Test
    void givenNonExpiredToken_whenGetValidBearerAccessToken_thenReturnToken() throws SpotifyTokenException {
        // given
        final String userId = "test-user-id";
        final String accessToken = "test-access-token";
        final SpotifyToken token = new SpotifyToken(userId, accessToken, "test-refresh-token", Instant.now().plusSeconds(3600));
        when(tokenRepository.findByUserId(userId)).thenReturn(token);

        // when
        String result = spotifyTokenService.getValidBearerAccessToken(userId);

        // then
        assertEquals("Bearer " + accessToken, result);
    }

    @Test
    void givenNewToken_whenCreateOrUpdateRefreshToken_thenCreateToken() {
        // given
        final String userId = "test-user-id";
        final String refreshToken = "test-refresh-token";
        when(tokenRepository.findByUserId(userId)).thenReturn(null);

        // when
        spotifyTokenService.createOrUpdateRefreshToken(userId, refreshToken);

        // then
        verify(tokenRepository).persist(any(SpotifyToken.class));
    }

    @Test
    void givenExistingToken_whenCreateOrUpdateRefreshToken_thenUpdateToken() {
        // given
        final String userId = "test-user-id";
        final String newRefreshToken = "test-refresh-token";
        final SpotifyToken token = new SpotifyToken(userId, "test-access-token", "old-refresh-token", Instant.now());
        when(tokenRepository.findByUserId(userId)).thenReturn(token);

        // when
        spotifyTokenService.createOrUpdateRefreshToken(userId, newRefreshToken);

        // then
        verify(tokenRepository, times(0)).persist(any(SpotifyToken.class));
        assertEquals(newRefreshToken, token.getRefreshToken());
    }
}

package com.windschief.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.security.Principal;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.windschief.spotify.SpotifyAccountsApi;
import com.windschief.spotify.model.TokenResponse;

import io.quarkus.security.identity.SecurityIdentity;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

public class SpotifyTokenServiceTest {
    private static final String USER_ID = "test-user-id";

    private final SpotifyTokenRepository tokenRepository = mock(SpotifyTokenRepository.class);
    private final SpotifyConfig spotifyConfig = mock(SpotifyConfig.class);
    private final SpotifyAccountsApi spotifyAccountsApi = mock(SpotifyAccountsApi.class);
    private final SecurityIdentity securityIdentity = mock(SecurityIdentity.class);
    private final SpotifyTokenService spotifyTokenService = new SpotifyTokenService(tokenRepository, spotifyConfig,
            spotifyAccountsApi, securityIdentity);

    @BeforeEach
    void setup() {
        when(spotifyConfig.clientId()).thenReturn("test-client-id");
        when(spotifyConfig.clientSecret()).thenReturn("test-client-secret");

        final Principal principal = mock(Principal.class);
        when(principal.getName()).thenReturn(USER_ID);
        when(securityIdentity.getPrincipal()).thenReturn(principal);
    }

    @Test
    void givenNoToken_whenGetValidBearerAccessToken_thenThrowException() {
        // given
        when(tokenRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());

        // when / then
        assertThrows(SpotifyTokenException.class, () -> spotifyTokenService.getValidBearerAccessToken(USER_ID));
    }

    @Test
    void givenNoRefreshToken_whenGetValidBearerAccessToken_thenThrowException() {
        // given
        final SpotifyToken token = new SpotifyToken(USER_ID, null, null, null);
        when(tokenRepository.findByUserId(USER_ID)).thenReturn(Optional.of(token));

        // when / then
        assertThrows(SpotifyTokenException.class, () -> spotifyTokenService.getValidBearerAccessToken(USER_ID));
    }

    @Test
    void givenExpiredToken_whenGetValidBearerAccessToken_thenRefreshToken()
            throws WebApplicationException, SpotifyTokenException {
        // given
        final String newAccessToken = "new-access-token";
        final SpotifyToken token = new SpotifyToken(USER_ID, null, "test-refresh-token", Instant.now().minusSeconds(1));
        when(tokenRepository.findByUserId(USER_ID)).thenReturn(Optional.of(token));

        TokenResponse tokenResponse = new TokenResponse(newAccessToken, "", 3600, USER_ID, "test-refresh-token");

        when(spotifyAccountsApi.refreshToken(
                "Basic " + Base64.getEncoder().encodeToString("test-client-id:test-client-secret".getBytes()),
                "refresh_token",
                "test-refresh-token",
                "test-client-id")).thenReturn(tokenResponse);

        // when
        String result = spotifyTokenService.getValidBearerAccessToken(USER_ID);

        // then
        assertEquals("Bearer " + newAccessToken, result);
        assertEquals(newAccessToken, token.getAccessToken());
        assertNotNull(token.getExpiresAt());
    }

    @Test
    void givenNonExpiredToken_whenGetValidBearerAccessToken_thenReturnToken() throws SpotifyTokenException {
        // given
        final String accessToken = "test-access-token";
        final SpotifyToken token = new SpotifyToken(USER_ID, accessToken, "test-refresh-token",
                Instant.now().plusSeconds(3600));
        when(tokenRepository.findByUserId(USER_ID)).thenReturn(Optional.of(token));

        // when
        String result = spotifyTokenService.getValidBearerAccessToken(USER_ID);

        // then
        assertEquals("Bearer " + accessToken, result);
    }

    @Test
    void givenNoToken_whenStoreRefreshToken_thenCreateTokenAndReturnCreated() {
        // given
        final String refreshToken = "test-refresh-token";
        when(tokenRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());

        // when
        Response response = spotifyTokenService.storeRefreshToken(refreshToken);

        // then
        assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());
        verify(tokenRepository).persist(any(SpotifyToken.class));
    }

    @Test
    void givenExistingToken_whenStoreRefreshToken_thenUpdateTokenAndReturnOk() {
        // given
        final String newRefreshToken = "test-refresh-token";
        final SpotifyToken token = new SpotifyToken(USER_ID, "test-access-token", "old-refresh-token", Instant.now());
        when(tokenRepository.findByUserId(USER_ID)).thenReturn(Optional.of(token));

        // when
        Response response = spotifyTokenService.storeRefreshToken(newRefreshToken);

        // then
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        verify(tokenRepository, times(0)).persist(any(SpotifyToken.class));
        assertEquals(newRefreshToken, token.getRefreshToken());
    }
}

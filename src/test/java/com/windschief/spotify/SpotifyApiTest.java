package com.windschief.spotify;

import com.windschief.spotify.model.SpotifyUser;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@QuarkusTest
class SpotifyApiTest {
    @Mock
    @RestClient
    SpotifyApi spotifyApi;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void givenMockedSpotifyApi_whenGetCurrentUser_thenVerifyCall() {
        // GIVEN
        SpotifyUser mockUser = new SpotifyUser("id", "John Doe", "johndoe", "US", null, null, null, null, null,
                "premium", null, null);
        String mockToken = "Bearer mock_token";
        when(spotifyApi.getCurrentUser(mockToken)).thenReturn(mockUser);

        // WHEN
        SpotifyUser user = spotifyApi.getCurrentUser(mockToken);

        // THEN
        assertEquals("id", user.id());
        assertEquals("John Doe", user.displayName());
        verify(spotifyApi).getCurrentUser(mockToken);
    }
}
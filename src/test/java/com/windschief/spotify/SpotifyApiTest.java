package com.windschief.spotify;

import com.windschief.spotify.model.SpotifyUser;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;

@QuarkusTest
class SpotifyApiTest {

    @InjectMock
    @RestClient
    SpotifyApi spotifyApi;

    @Test
    void givenMockedSpotifyApi_whenGetCurrentUser_thenVerifyCall() {
        // GIVEN
        SpotifyUser mockUser = new SpotifyUser("id", "John Doe", "johndoe", "US", null, null, null, null, null, "premium", null, null);
        String mockToken = "Bearer mock_token";
        Mockito.when(spotifyApi.getCurrentUser(mockToken)).thenReturn(mockUser);

        // WHEN
        SpotifyUser user = spotifyApi.getCurrentUser(mockToken);

        // THEN
        assertEquals("id", user.id());
        assertEquals("John Doe", user.displayName());
        Mockito.verify(spotifyApi).getCurrentUser(mockToken);
    }
}
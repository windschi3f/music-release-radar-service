package com.windschief.spotify;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import jakarta.ws.rs.Path;

@Path("/v1")
@RegisterRestClient(configKey="spotify-api")
public interface SpotifyApi {
}

package com.windschief.spotify;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import com.windschief.spotify.model.SpotifyUser;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.Path;

@Path("/v1")
@RegisterRestClient(configKey="spotify-api")
public interface SpotifyApi {
    @GET
    @Path("/me")
    SpotifyUser getCurrentUser(@HeaderParam("Authorization") String authHeader);

    @GET
    @Path("/me/following")
    SpotifyUser getFollowing(@HeaderParam("Authorization") String authHeader);
}

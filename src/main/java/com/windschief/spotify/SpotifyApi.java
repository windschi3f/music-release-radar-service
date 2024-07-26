package com.windschief.spotify;

import com.windschief.spotify.model.AlbumsResponse;
import com.windschief.spotify.model.FollowingResponse;
import com.windschief.spotify.model.SearchResponse;

import com.windschief.spotify.model.SpotifyUser;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;

@Path("/v1")
@RegisterRestClient(configKey="spotify-api")
public interface SpotifyApi {
    @GET
    @Path("/me")
    SpotifyUser getCurrentUser(@HeaderParam("Authorization") String authHeader);

    @GET
    @Path("/me/following")
    FollowingResponse getFollowing(@HeaderParam("Authorization") String authHeader);

    @GET
    @Path("/artists/{id}/albums")
    AlbumsResponse getArtistAlbums(
            @HeaderParam("Authorization") String authHeader,
            @PathParam("id") String artistId,
            @QueryParam("include_groups") String includeGroups,
            @QueryParam("limit") int limit,
            @QueryParam("offset") int offset
    );

    @GET
    @Path("/search")
    SearchResponse search(
            @HeaderParam("Authorization") String authHeader,
            @QueryParam("q") String query,
            @QueryParam("type") String type,
            @QueryParam("market") String market,
            @QueryParam("limit") int limit,
            @QueryParam("offset") int offset,
            @QueryParam("include_external") String includeExternal
    );
}

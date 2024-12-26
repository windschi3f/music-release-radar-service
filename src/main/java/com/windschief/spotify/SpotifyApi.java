package com.windschief.spotify;

import com.windschief.spotify.model.AlbumsResponse;
import com.windschief.spotify.model.FollowingResponse;
import com.windschief.spotify.model.SearchResponse;

import com.windschief.spotify.model.SpotifyUser;
import com.windschief.spotify.model.TokenResponse;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

@Path("/v1")
@RegisterRestClient(configKey = "spotify-api")
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
                        @QueryParam("offset") int offset);

        @GET
        @Path("/search")
        SearchResponse search(
                        @HeaderParam("Authorization") String authHeader,
                        @QueryParam("q") String query,
                        @QueryParam("type") String type,
                        @QueryParam("market") String market,
                        @QueryParam("limit") int limit,
                        @QueryParam("offset") int offset,
                        @QueryParam("include_external") String includeExternal);

        @POST
        @Path("/api/token")
        @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
        @Produces(MediaType.APPLICATION_JSON)
        TokenResponse refreshToken(
                        @HeaderParam("Authorization") String basicAuth,
                        @FormParam("grant_type") String grantType,
                        @FormParam("refresh_token") String refreshToken);

        @GET
        <T> T getNextPage(@HeaderParam("Authorization") String authHeader, Class<T> responseType);
}

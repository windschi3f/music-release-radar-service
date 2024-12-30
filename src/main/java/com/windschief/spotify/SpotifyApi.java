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
        /**
         * Get the current user's Spotify profile.
         *
         * @param authHeader The authorization header containing the access token.
         * @return The user's Spotify profile information.
         * @throws Exception if the request fails.
         */
        @GET
        @Path("/me")
        SpotifyUser getCurrentUser(@HeaderParam("Authorization") String authHeader) throws Exception;

        /**
         * Get the artists that the current user follows.
         *
         * @param authHeader The authorization header containing the access token.
         * @return Information about the artists the user follows.
         * @throws Exception if the request fails.
         */
        @GET
        @Path("/me/following")
        FollowingResponse getFollowing(@HeaderParam("Authorization") String authHeader) throws Exception;

        /**
         * Get an artist's albums.
         *
         * @param authHeader    The authorization header containing the access token.
         * @param artistId      The Spotify ID of the artist.
         * @param includeGroups A comma-separated list of keywords that will be used to
         *                      filter the response. Valid values are: album, single,
         *                      appears_on, compilation.
         * @param limit         The maximum number of items to return. Default: 20.
         *                      Minimum: 1. Maximum: 50.
         * @param offset        The index of the first item to return. Default: 0.
         * @return The artist's albums.
         * @throws Exception if the request fails.
         */
        @GET
        @Path("/artists/{id}/albums")
        AlbumsResponse getArtistAlbums(
                        @HeaderParam("Authorization") String authHeader,
                        @PathParam("id") String artistId,
                        @QueryParam("include_groups") String includeGroups,
                        @QueryParam("limit") int limit,
                        @QueryParam("offset") int offset) throws Exception;

        /**
         * Search for Spotify items.
         *
         * @param authHeader      The authorization header containing the access token.
         * @param query           Search query keywords.
         * @param type            A comma-separated list of item types to search across.
         *                        Valid values are: album, artist, playlist, track.
         * @param market          An ISO 3166-1 alpha-2 country code or 'from_token'.
         * @param limit           The maximum number of items to return. Default: 20.
         *                        Minimum: 1. Maximum: 50.
         * @param offset          The index of the first item to return. Default: 0.
         * @param includeExternal If 'audio' is specified, audio features will be
         *                        included.
         * @return Search results.
         * @throws Exception if the request fails.
         */
        @GET
        @Path("/search")
        SearchResponse search(
                        @HeaderParam("Authorization") String authHeader,
                        @QueryParam("q") String query,
                        @QueryParam("type") String type,
                        @QueryParam("market") String market,
                        @QueryParam("limit") int limit,
                        @QueryParam("offset") int offset,
                        @QueryParam("include_external") String includeExternal) throws Exception;

        /**
         * Refresh an access token.
         *
         * @param basicAuth    Base64 encoded string of client_id:client_secret.
         * @param grantType    Must be set to "refresh_token".
         * @param refreshToken The refresh token returned from the authorization code
         *                     exchange.
         * @return A new access token.
         * @throws Exception if the request fails.
         */
        @POST
        @Path("/api/token")
        @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
        @Produces(MediaType.APPLICATION_JSON)
        TokenResponse refreshToken(
                        @HeaderParam("Authorization") String basicAuth,
                        @FormParam("grant_type") String grantType,
                        @FormParam("refresh_token") String refreshToken) throws Exception;

        /**
         * Adds items to a Spotify playlist.
         *
         * @param authHeader The authorization header containing the access token.
         * @param playlistId The ID of the playlist to add items to.
         * @param uris       A comma-separated list of Spotify URIs (tracks or episodes)
         *                   to add. Maximum 100 URIs per request.
         * @param position   The position to insert the items (zero-based index).
         *                   If omitted, items will be appended.
         * @throws Exception if the request fails.
         */
        @POST
        @Path("/playlists/{playlistId}/tracks")
        @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
        void addToPlaylist(
                        @HeaderParam("Authorization") String authHeader,
                        @PathParam("playlistId") String playlistId,
                        @FormParam("uris") String uris,
                        @FormParam("position") Integer position) throws Exception;

        /**
         * Get the next page of a paginated response.
         *
         * @param authHeader   The authorization header containing the access token.
         * @param responseType The class type of the expected response.
         * @return The next page of results.
         * @throws Exception if the request fails.
         */
        @GET
        <T> T getNextPage(@HeaderParam("Authorization") String authHeader, Class<T> responseType);
}

package com.windschief.spotify;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import com.windschief.spotify.model.TokenResponse;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;

@Path("/")
@RegisterRestClient(configKey = "spotify-accounts-api") 
@ApplicationScoped
public interface SpotifyAccountsApi {
    /**
     * Refresh an access token.
     *
     * @param basicAuth    Base64 encoded string of client_id:client_secret.
     * @param grantType    Must be set to "refresh_token".
     * @param refreshToken The refresh token returned from the authorization code
     *                     exchange.
     * @param clientId     The client ID of the application.
     * @return A new access token.
     * @throws WebApplicationException if the request fails.
     */
    @POST
    @Path("/token")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_JSON)
    TokenResponse refreshToken(
        @HeaderParam("Authorization") String basicAuth,
        @FormParam("grant_type") String grantType,
        @FormParam("refresh_token") String refreshToken,
        @FormParam("client_id") String clientId) throws WebApplicationException;
}
package com.windschief.auth;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import io.quarkus.security.Authenticated;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/auth/spotify")
@Tag(name = "Spotify Authentication", description = "Operations for managing Spotify authentication")
public interface SpotifyTokenApi {

    @PUT
    @Path("/refresh-token")
    @Consumes(MediaType.TEXT_PLAIN)
    @Operation(summary = "Store a refresh token for the current user")
    @APIResponse(responseCode = "200", description = "Refresh token updated")
    @APIResponse(responseCode = "201", description = "Refresh token stored")
    @APIResponse(responseCode = "401", description = "Unauthorized")
    @Authenticated
    Response storeRefreshToken(@RequestBody String refreshToken);
}

package com.windschief.user;

import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import io.quarkus.security.Authenticated;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/users")
@Tag(name = "User Management", description = "Operations for managing user data")
public interface UserApi {
    @GET
    @Path("/me/data")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Get all user data (GDPR data export)")
    @APIResponse(
        responseCode = "200", 
        description = "User data retrieved successfully",
        content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserDataDto.class))
    )
    @APIResponse(responseCode = "401", description = "Unauthorized")
    @Authenticated
    Response getUserData();

    @DELETE
    @Path("/me")
    @Operation(summary = "Delete all user data")
    @APIResponse(responseCode = "204", description = "User data deleted successfully")
    @APIResponse(responseCode = "401", description = "Unauthorized")
    @Authenticated
    Response deleteUserData();
}

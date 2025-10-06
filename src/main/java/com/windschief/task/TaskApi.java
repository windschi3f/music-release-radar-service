package com.windschief.task;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import io.quarkus.security.Authenticated;

import java.util.List;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

@Path("/tasks")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Task Management", description = "Operations for managing tasks")
public interface TaskApi {

        @GET
        @Operation(summary = "Get all tasks")
        @APIResponse(responseCode = "200", description = "Successful operation", content = @Content(mediaType = "application/json", schema = @Schema(implementation = TaskResponseDto.class, type = SchemaType.ARRAY)))
        @Authenticated
        List<TaskResponseDto> getTasks();

        @GET
        @Path("/{id}")
        @Operation(summary = "Get a task by ID")
        @APIResponses(value = {
                        @APIResponse(responseCode = "200", description = "Successful operation", content = @Content(mediaType = "application/json", schema = @Schema(implementation = TaskResponseDto.class))),
                        @APIResponse(responseCode = "401", description = "Unauthorized"),
                        @APIResponse(responseCode = "404", description = "Task not found")
        })
        @Authenticated
        Response getTask(@Parameter(description = "ID of the task", required = true) @PathParam("id") Long id);

        @POST
        @Path("/{id}/execute")
        @Operation(summary = "Execute a task by ID")
        @APIResponses(value = {
                        @APIResponse(responseCode = "202", description = "Task execution started"),
                        @APIResponse(responseCode = "401", description = "Unauthorized"),
                        @APIResponse(responseCode = "404", description = "Task not found")
        })
        @Authenticated
        Response executeTask(@Parameter(description = "ID of the task", required = true) @PathParam("id") Long id);

        @POST
        @Operation(summary = "Create a new task")
        @APIResponse(responseCode = "201", description = "Task created", content = @Content(mediaType = "application/json", schema = @Schema(implementation = TaskResponseDto.class)))
        @Authenticated
        Response createTask(
                        @Parameter(description = "Task to be created", required = true, schema = @Schema(implementation = TaskRequestDto.class)) TaskRequestDto taskRequestDto);

        @PUT
        @Path("/{id}")
        @Operation(summary = "Update an existing task")
        @APIResponses(value = {
                        @APIResponse(responseCode = "200", description = "Successful operation", content = @Content(mediaType = "application/json", schema = @Schema(implementation = TaskResponseDto.class))),
                        @APIResponse(responseCode = "401", description = "Unauthorized"),
                        @APIResponse(responseCode = "404", description = "Task not found"),
        })
        @Authenticated
        Response updateTask(
                        @Parameter(description = "ID of the task to be updated", required = true) @PathParam("id") Long id,
                        @Parameter(description = "Updated task information", required = true, schema = @Schema(implementation = TaskRequestDto.class)) TaskRequestDto taskRequestDto);

        @DELETE
        @Path("/{id}")
        @Operation(summary = "Delete a task")
        @APIResponses(value = {
                        @APIResponse(responseCode = "204", description = "Successful operation"),
                        @APIResponse(responseCode = "404", description = "Task not found")
        })
        @Authenticated
        Response deleteTask(
                        @Parameter(description = "ID of the task to be deleted", required = true) @PathParam("id") Long id);
}
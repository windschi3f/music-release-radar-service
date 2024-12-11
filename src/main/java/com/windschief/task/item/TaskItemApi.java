package com.windschief.task.item;

import java.util.List;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import com.windschief.task.TaskRequestDto;

import io.quarkus.security.Authenticated;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

@Path("/tasks/{taskId}/items")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Task Item Management", description = "Operations for managing tasks")
public interface TaskItemApi {

        @GET
        @Operation(summary = "Get all task items")
        @APIResponse(responseCode = "200", description = "Successful operation", content = @Content(mediaType = "application/json", schema = @Schema(implementation = TaskItemResponseDto.class, type = SchemaType.ARRAY)))
        @Authenticated
        List<TaskItemResponseDto> getTaskItems(
                        @Parameter(description = "ID of the task", required = true) @PathParam("taskId") Long taskId);

        @POST
        @Operation(summary = "Create new task items")
        @APIResponses(value = {
                        @APIResponse(responseCode = "201", description = "Task items created", content = @Content(mediaType = "application/json", schema = @Schema(implementation = TaskItemResponseDto.class, type = SchemaType.ARRAY))),
                        @APIResponse(responseCode = "400", description = "Invalid input"),
                        @APIResponse(responseCode = "401", description = "Unauthorized"),
                        @APIResponse(responseCode = "404", description = "Task not found")
        })
        @Authenticated
        Response createTaskItems(
                        @Parameter(description = "ID of the task", required = true) @PathParam("taskId") Long taskId,
                        @Parameter(description = "Task items to be created", required = true, schema = @Schema(implementation = TaskRequestDto.class, type = SchemaType.ARRAY)) List<TaskItemRequestDto> taskItemRequestDtos);

        @DELETE
        @Path("/{taskItemId}")
        @Operation(summary = "Delete a task item by ID")
        @APIResponses(value = {
                        @APIResponse(responseCode = "204", description = "Task item deleted"),
                        @APIResponse(responseCode = "404", description = "Task item not found")
        })
        @Authenticated
        Response deleteTaskItem(
                        @Parameter(description = "ID of the task", required = true) @PathParam("taskId") Long taskId,
                        @Parameter(description = "ID of the task item", required = true) @PathParam("taskItemId") Long taskItemId);

}
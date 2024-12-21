package com.windschief.task.added_item;

import com.windschief.task.Task;
import com.windschief.task.TaskRepository;

import io.quarkus.security.identity.SecurityIdentity;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;

@RequestScoped
public class AddedItemService implements AddedItemApi {
    private final SecurityIdentity securityIdentity;
    private final TaskRepository taskRepository;
    private final AddedItemRepository addedItemRepository;

    @Inject
    public AddedItemService(SecurityIdentity securityIdentity,
            TaskRepository taskRepository,
            AddedItemRepository addedItemRepository) {
        this.securityIdentity = securityIdentity;
        this.taskRepository = taskRepository;
        this.addedItemRepository = addedItemRepository;
    }

    @Override
    public Response getAddedItems(Long taskId) {
        Task task = taskRepository.findById(taskId);

        if (task == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        } else if (!task.getUserId().equals(securityIdentity.getPrincipal().getName())) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }

        return Response.ok(addedItemRepository.findByTaskId(taskId).stream()
                .map(AddedItemResponseDto::from)
                .toList())
                .build();
    }
}

package com.windschief.task.added_item;

import com.windschief.task.Task;
import com.windschief.task.TaskAccess;
import com.windschief.task.TaskRepository;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;

@RequestScoped
public class AddedItemService implements AddedItemApi {
    private final TaskAccess taskAccess;
    private final TaskRepository taskRepository;
    private final AddedItemRepository addedItemRepository;

    @Inject
    public AddedItemService(TaskAccess taskAccess,
            TaskRepository taskRepository,
            AddedItemRepository addedItemRepository) {
        this.taskAccess = taskAccess;
        this.taskRepository = taskRepository;
        this.addedItemRepository = addedItemRepository;
    }

    @Override
    public Response getAddedItems(Long taskId) {
        Task task = taskRepository.findById(taskId);

        taskAccess.checkAccess(task);

        return Response.ok(addedItemRepository.findByTaskId(taskId).stream()
                .map(AddedItemResponseDto::from)
                .toList())
                .build();
    }
}

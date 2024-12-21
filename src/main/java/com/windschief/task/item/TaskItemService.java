package com.windschief.task.item;

import java.util.List;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.core.Response;

import com.windschief.task.Task;
import com.windschief.task.TaskItemRepository;
import com.windschief.task.TaskRepository;
import io.quarkus.security.identity.SecurityIdentity;

@RequestScoped
public class TaskItemService implements TaskItemApi {

    private final SecurityIdentity securityIdentity;
    private final TaskRepository taskRepository;
    private final TaskItemRepository taskItemRepository;

    @Inject
    public TaskItemService(SecurityIdentity securityIdentity, TaskRepository taskRepository,
            TaskItemRepository taskItemRepository) {
        this.securityIdentity = securityIdentity;
        this.taskRepository = taskRepository;
        this.taskItemRepository = taskItemRepository;
    }

    @Override
    public Response getTaskItems(Long taskId) {
        Task task = taskRepository.findById(taskId);
        if (task == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        } else if (!task.getUserId().equals(securityIdentity.getPrincipal().getName())) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }

        return Response.ok(taskItemRepository.findByTaskId(taskId).stream()
                .map(TaskItemResponseDto::from)
                .toList())
                .build();
    }

    @Override
    @Transactional
    public Response createTaskItems(Long taskId, List<TaskItemRequestDto> taskItemRequestDtos) {
        Task task = taskRepository.findById(taskId);
        if (task == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        } else if (!task.getUserId().equals(securityIdentity.getPrincipal().getName())) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }

        List<TaskItem> taskItems = taskItemRequestDtos.stream()
                .map(dto -> {
                    TaskItem taskItem = TaskItemRequestDto.toTaskItem(dto);
                    taskItem.setTask(task);
                    return taskItem;
                })
                .toList();

        taskItemRepository.persist(taskItems);

        return Response.status(Response.Status.CREATED)
                .entity(taskItems.stream()
                        .map(TaskItemResponseDto::from)
                        .toList())
                .build();
    }

    @Override
    public Response deleteTaskItem(Long taskId, Long taskItemId) {
        long deletedCount = taskItemRepository.deleteByTaskIdAndTaskItemIdAndUserId(taskId, taskItemId,
                securityIdentity.getPrincipal().getName());
        if (deletedCount == 0) {
            return Response.status(Response.Status.NOT_FOUND).build();
        } else {
            return Response.noContent().build();
        }
    }
}

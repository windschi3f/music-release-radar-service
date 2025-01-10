package com.windschief.task.item;

import java.util.List;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.core.Response;
import com.windschief.task.Task;
import com.windschief.task.TaskItemRepository;
import com.windschief.task.TaskRepository;
import com.windschief.task.TaskAccess;

@RequestScoped
public class TaskItemService implements TaskItemApi {
    private final TaskAccess taskAccess;
    private final TaskRepository taskRepository;
    private final TaskItemRepository taskItemRepository;

    @Inject
    public TaskItemService(TaskAccess taskAccess, TaskRepository taskRepository,
            TaskItemRepository taskItemRepository) {
        this.taskAccess = taskAccess;
        this.taskRepository = taskRepository;
        this.taskItemRepository = taskItemRepository;
    }

    @Override
    public Response getTaskItems(Long taskId) {
        Task task = taskRepository.findById(taskId);

        taskAccess.checkAccess(task);

        return Response.ok(taskItemRepository.findByTaskId(taskId).stream()
                .map(TaskItemResponseDto::from)
                .toList())
                .build();
    }

    @Override
    @Transactional
    public Response createTaskItems(Long taskId, List<TaskItemRequestDto> taskItemRequestDtos) {
        Task task = taskRepository.findById(taskId);

        taskAccess.checkAccess(task);

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
    @Transactional
    public Response replaceTaskItems(Long taskId, List<TaskItemRequestDto> taskItemRequestDtos) {
        Task task = taskRepository.findById(taskId);
        taskAccess.checkAccess(task);
        
        taskItemRepository.deleteByTaskId(taskId);
        
        final List<TaskItem> taskItems = taskItemRequestDtos.stream()
            .map(dto -> {
                TaskItem taskItem = TaskItemRequestDto.toTaskItem(dto);
                taskItem.setTask(task);
                return taskItem;
            })
            .toList();
            
        taskItemRepository.persist(taskItems);
        
        return Response.ok(taskItems.stream()
            .map(TaskItemResponseDto::from)
            .toList())
            .build();
    }

    @Override
    @Transactional
    public Response deleteTaskItem(Long taskId, Long taskItemId) {
        Task task = taskRepository.findById(taskId);

        taskAccess.checkAccess(task);

        long deletedCount = taskItemRepository.deleteByTaskIdAndTaskItemIdAndUserId(taskId, taskItemId,
                taskAccess.getCurrentUserId());
        if (deletedCount == 0) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.noContent().build();
    }
}
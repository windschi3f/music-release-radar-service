package com.windschief.task.service;

import java.util.List;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.core.Response;

import com.windschief.task.TaskRepository;
import com.windschief.task.api.TaskApi;
import com.windschief.task.domain.Task;
import com.windschief.task.dto.TaskRequestDto;
import com.windschief.task.dto.TaskResponseDto;
import io.quarkus.security.identity.SecurityIdentity;

@RequestScoped
public class TaskService implements TaskApi {

    private final SecurityIdentity securityIdentity;
    private final TaskRepository taskRepository;

    @Inject
    public TaskService(SecurityIdentity securityIdentity, TaskRepository taskRepository) {
        this.securityIdentity = securityIdentity;
        this.taskRepository = taskRepository;
    }

    @Override
    public List<TaskResponseDto> getTasks() {
        return taskRepository.findByUserId(securityIdentity.getPrincipal().getName()).stream()
                .map(TaskResponseDto::from)
                .toList();
    }

    @Override
    public Response getTask(Long id) {
        Task task = taskRepository.findById(id);
        if (task == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        } else if (!task.getUserId().equals(securityIdentity.getPrincipal().getName())) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        } else {
            return Response.ok(TaskResponseDto.from(task)).build();
        }
    }

    @Override
    public TaskResponseDto createTask(TaskRequestDto taskRequestDto) {
        Task task = TaskRequestDto.toTask(taskRequestDto);
        task.setUserId(securityIdentity.getPrincipal().getName());
        taskRepository.persist(task);

        return TaskResponseDto.from(task);
    }

    @Override
    @Transactional
    public Response updateTask(Long id, TaskRequestDto taskRequestDto) {
        Task task = taskRepository.findById(id);
        if (task == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        } else if (!task.getUserId().equals(securityIdentity.getPrincipal().getName())) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        } else {
            Task updateTask = TaskRequestDto.toTask(taskRequestDto);
            updateTask.setId(id);
            updateTask.setUserId(securityIdentity.getPrincipal().getName());
            updateTask.setLastTimeExecuted(task.getLastTimeExecuted());
            taskRepository.persist(updateTask);
            return Response.ok(TaskResponseDto.from(updateTask)).build();
        }
    }

    @Override
    public Response deleteTask(Long id) {
        long deletedCount = taskRepository.deleteByTaskIdAndUserId(id, securityIdentity.getPrincipal().getName());
        if (deletedCount == 0) {
            return Response.status(Response.Status.NOT_FOUND).build();
        } else {
            return Response.noContent().build();
        }
    }
}

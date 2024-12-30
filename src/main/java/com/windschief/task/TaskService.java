package com.windschief.task;

import java.util.List;
import java.util.Optional;

import com.windschief.auth.SpotifyTokenService;

import io.quarkus.security.identity.SecurityIdentity;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.core.Response;

@RequestScoped
public class TaskService implements TaskApi {

    private final TaskAccess taskAccess;
    private final TaskRepository taskRepository;
    private final SpotifyTokenService spotifyTokenService;

    @Inject
    public TaskService(TaskAccess taskAccess, TaskRepository taskRepository, SpotifyTokenService spotifyTokenService) {
        this.taskAccess = taskAccess;
        this.taskRepository = taskRepository;
        this.spotifyTokenService = spotifyTokenService;
    }

    @Override
    public List<TaskResponseDto> getTasks() {
        return taskRepository.findByUserId(taskAccess.getCurrentUserId()).stream()
                .map(TaskResponseDto::from)
                .toList();
    }

    @Override
    public Response getTask(Long id) {
        Task task = taskRepository.findById(id);

        Optional<Response> accessCheck = taskAccess.checkAccess(task);
        if (accessCheck.isPresent()) {
            return accessCheck.get();
        } else {
            return Response.ok(TaskResponseDto.from(task)).build();
        }
    }

    @Override
    public TaskResponseDto createTask(TaskRequestDto taskRequestDto) {
        if (taskRequestDto.refreshToken() == null) {
            throw new IllegalArgumentException("refreshToken is required");
        }

        Task task = TaskRequestDto.toTask(taskRequestDto);
        task.setUserId(taskAccess.getCurrentUserId());
        taskRepository.persist(task);

        final SecurityIdentity securityIdentity = taskAccess.getSecurityIdentity();
        spotifyTokenService.updateStoredToken(
                securityIdentity.getPrincipal().getName(),
                securityIdentity.getAttribute("spotifyToken"),
                taskRequestDto.refreshToken());

        return TaskResponseDto.from(task);
    }

    @Override
    @Transactional
    public Response updateTask(Long id, TaskRequestDto taskRequestDto) {
        Task task = taskRepository.findById(id);

        Optional<Response> accessCheck = taskAccess.checkAccess(task);
        if (accessCheck.isPresent()) {
            return accessCheck.get();
        } else {
            Task updateTask = TaskRequestDto.toTask(taskRequestDto);
            updateTask.setId(id);
            updateTask.setUserId(taskAccess.getCurrentUserId());
            updateTask.setLastTimeExecuted(task.getLastTimeExecuted());
            taskRepository.persist(updateTask);
            return Response.ok(TaskResponseDto.from(updateTask)).build();
        }
    }

    @Override
    public Response deleteTask(Long id) {
        long deletedCount = taskRepository.deleteByTaskIdAndUserId(id, taskAccess.getCurrentUserId());
        if (deletedCount == 0) {
            return Response.status(Response.Status.NOT_FOUND).build();
        } else {
            return Response.noContent().build();
        }
    }
}

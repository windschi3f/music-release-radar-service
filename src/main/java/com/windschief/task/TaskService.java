package com.windschief.task;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import com.windschief.auth.SpotifyTokenService;
import com.windschief.releasedetection.ReleaseRadarService;

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
    private final ReleaseRadarService releaseRadarService;

    @Inject
    public TaskService(TaskAccess taskAccess, TaskRepository taskRepository, SpotifyTokenService spotifyTokenService,
            ReleaseRadarService releaseRadarService) {
        this.taskAccess = taskAccess;
        this.taskRepository = taskRepository;
        this.spotifyTokenService = spotifyTokenService;
        this.releaseRadarService = releaseRadarService;
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

        taskAccess.checkAccess(task);

        return Response.ok(TaskResponseDto.from(task)).build();
    }

    @Override
    public Response executeTask(Long id) {
        Task task = taskRepository.findById(id);

        taskAccess.checkAccess(task);

        task.getTaskItems().size();
        CompletableFuture.runAsync(() -> releaseRadarService.execute(task));

        return Response.accepted().build();
    }

    @Override
    @Transactional
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

        taskAccess.checkAccess(task);

        TaskRequestDto.updateTask(task, taskRequestDto);
        return Response.ok(TaskResponseDto.from(task)).build();
    }

    @Override
    @Transactional
    public Response deleteTask(Long id) {
        long deletedCount = taskRepository.deleteByTaskIdAndUserId(id, taskAccess.getCurrentUserId());
        if (deletedCount == 0) {
            return Response.status(Response.Status.NOT_FOUND).build();
        } else {
            return Response.noContent().build();
        }
    }
}

package com.windschief.task;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import com.windschief.auth.SpotifyTokenService;
import com.windschief.releasedetection.ReleaseRadarService;
import com.windschief.task.added_item.AddedItemRepository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.core.Response;

@ApplicationScoped
public class TaskService implements TaskApi {
    private final TaskAccess taskAccess;
    private final TaskRepository taskRepository;
    private final AddedItemRepository addedItemRepository;
    private final SpotifyTokenService spotifyTokenService;
    private final ReleaseRadarService releaseRadarService;
    private final TaskMapper taskMapper;

    @Inject
    public TaskService(TaskAccess taskAccess, TaskRepository taskRepository, AddedItemRepository addedItemRepository,
            SpotifyTokenService spotifyTokenService, ReleaseRadarService releaseRadarService, TaskMapper taskMapper) {
        this.taskAccess = taskAccess;
        this.taskRepository = taskRepository;
        this.addedItemRepository = addedItemRepository;
        this.spotifyTokenService = spotifyTokenService;
        this.releaseRadarService = releaseRadarService;
        this.taskMapper = taskMapper;
    }

    @Override
    public List<TaskResponseDto> getTasks() {
        return taskRepository.findByUserId(taskAccess.getCurrentUserId()).stream()
                .map(taskMapper::toDto)
                .toList();
    }

    @Override
    public Response getTask(Long id) {
        Task task = taskRepository.findById(id);

        taskAccess.checkAccess(task);

        return Response.ok(taskMapper.toDto(task)).build();
    }

    @Override
    public Response executeTask(Long id) {
        Task task = taskRepository.findById(id);

        taskAccess.checkAccess(task);

        CompletableFuture.runAsync(() -> releaseRadarService.execute(task));

        return Response.accepted().build();
    }

    @Override
    @Transactional
    public Response createTask(TaskRequestDto taskRequestDto) {
        if (!spotifyTokenService.hasRefreshToken(taskAccess.getSecurityIdentity().getPrincipal().getName())) {
            return Response.status(Response.Status.PRECONDITION_FAILED)
                    .entity("Refresh token must be saved before creating tasks")
                    .build();
        }

        Task task = TaskRequestDto.toTask(taskRequestDto);
        task.setUserId(taskAccess.getCurrentUserId());
        taskRepository.persist(task);

        return Response.status(Response.Status.CREATED)
                .entity(taskMapper.toDto(task))
                .build();
    }

    @Override
    @Transactional
    public Response updateTask(Long id, TaskRequestDto taskRequestDto) {
        Task task = taskRepository.findById(id);

        taskAccess.checkAccess(task);

        if (releaseRadarService.isTaskProcessing(id)) {
            throw new IllegalStateException("Task execution is in progress");
        }

        TaskRequestDto.updateTask(task, taskRequestDto);
        return Response.ok(taskMapper.toDto(task)).build();
    }

    @Override
    @Transactional
    public Response deleteTask(Long id) {
        Task task = taskRepository.findById(id);

        taskAccess.checkAccess(task);

        if (releaseRadarService.isTaskProcessing(id)) {
            throw new IllegalStateException("Task execution is in progress");
        }

        addedItemRepository.deleteByTaskIdAndUserId(id, taskAccess.getCurrentUserId());
        taskRepository.delete(task); // cascades to task items

        return Response.noContent().build();
    }
}

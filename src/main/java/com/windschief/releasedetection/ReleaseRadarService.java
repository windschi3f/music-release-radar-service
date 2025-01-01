package com.windschief.releasedetection;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.microprofile.rest.client.inject.RestClient;

import com.windschief.auth.SpotifyTokenService;
import com.windschief.spotify.SpotifyApi;
import com.windschief.task.Task;
import com.windschief.task.TaskRepository;

import io.quarkus.logging.Log;
import io.quarkus.scheduler.Scheduled;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.WebApplicationException;

public class ReleaseRadarService {
    private static final int CHUNK_SIZE = 100;

    private final ReleaseDetectionService releaseDetectionService;
    private final TaskRepository taskRepository;
    private final SpotifyTokenService spotifyTokenService;
    private final SpotifyApi spotifyApi;

    @Inject
    public ReleaseRadarService(
            ReleaseDetectionService releaseDetectionService,
            TaskRepository taskRepository,
            SpotifyTokenService spotifyTokenService,
            @RestClient SpotifyApi spotifyApi) {
        this.releaseDetectionService = releaseDetectionService;
        this.taskRepository = taskRepository;
        this.spotifyTokenService = spotifyTokenService;
        this.spotifyApi = spotifyApi;
    }

    @Transactional
    @Scheduled(every = "24h")
    public void addNewReleases() {
        Instant startTime = Instant.now();
        Log.info("Starting release radar job");

        List<Task> tasks = taskRepository.findAll().stream()
                .filter(Task::isActive)
                .filter(Task::isDue)
                .toList();

        tasks.forEach(this::execute);

        Log.info(String.format("Release radar job completed in %s seconds for %d tasks",
                Duration.between(startTime, Instant.now()), tasks.size()));
    }

    @Transactional
    public void execute(Long taskId) {
        Task task = taskRepository.findById(taskId);
        execute(task);
    }

    private void execute(Task task) {
        try {
            Set<String> newAlbumIds = releaseDetectionService.detectNewAlbumIds(task);
            if (newAlbumIds.isEmpty()) {
                return;
            }

            String accessToken = spotifyTokenService.getValidToken(task.getUserId());
            addAlbumsToPlaylist(task, newAlbumIds, accessToken);

            task.setLastTimeExecuted(Instant.now());
            taskRepository.persist(task);
        } catch (WebApplicationException | IOException | InterruptedException e) {
            Log.error(String.format("Failed to execute task [taskId=%s, userId=%s, playlistId=%s]",
                    task.getId(), task.getUserId(), task.getExternalDestinationId()), e);
        }
    }

    private void addAlbumsToPlaylist(Task task, Set<String> newAlbumIds, String accessToken)
            throws WebApplicationException {
        for (int i = 0; i < newAlbumIds.size(); i += CHUNK_SIZE) {
            final String idsChunk = newAlbumIds.stream()
                    .skip(i)
                    .limit(CHUNK_SIZE)
                    .collect(Collectors.joining(","));
            spotifyApi.addToPlaylist(accessToken, task.getExternalDestinationId(), idsChunk, null);
        }
    }
}
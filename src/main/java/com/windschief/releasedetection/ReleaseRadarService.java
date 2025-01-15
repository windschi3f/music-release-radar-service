package com.windschief.releasedetection;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

import org.eclipse.microprofile.rest.client.inject.RestClient;

import com.windschief.auth.SpotifyTokenService;
import com.windschief.spotify.SpotifyApi;
import com.windschief.spotify.model.PlaylistAddItemsRequest;
import com.windschief.spotify.model.TrackItem;
import com.windschief.task.Task;
import com.windschief.task.TaskRepository;
import com.windschief.task.added_item.AddedItem;
import com.windschief.task.added_item.AddedItemRepository;

import io.quarkus.logging.Log;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.WebApplicationException;

@ApplicationScoped
public class ReleaseRadarService {
    private static final int CHUNK_SIZE = 100;

    private final ReleaseDetectionService releaseDetectionService;
    private final TaskRepository taskRepository;
    private final SpotifyTokenService spotifyTokenService;
    private final SpotifyApi spotifyApi;
    private final AddedItemRepository addedItemRepository;

    private final ConcurrentMap<Long, Boolean> processingTasks = new ConcurrentHashMap<>();

    @Inject
    public ReleaseRadarService(
            ReleaseDetectionService releaseDetectionService,
            TaskRepository taskRepository,
            SpotifyTokenService spotifyTokenService,
            @RestClient SpotifyApi spotifyApi,
            AddedItemRepository addedItemRepository) {
        this.releaseDetectionService = releaseDetectionService;
        this.taskRepository = taskRepository;
        this.spotifyTokenService = spotifyTokenService;
        this.spotifyApi = spotifyApi;
        this.addedItemRepository = addedItemRepository;
    }

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
                Duration.between(startTime, Instant.now()).toSeconds(), tasks.size()));
    }

    public void execute(Task task) {
        final long taskId = task.getId();
        processingTasks.put(taskId, true);

        try {
            final Set<String> newReleaseTrackUris = releaseDetectionService.detectNewReleaseTracks(taskId)
                    .stream()
                    .map(TrackItem::uri)
                    .collect(Collectors.toSet());
            if (newReleaseTrackUris.isEmpty()) {
                return;
            }

            final String bearerToken = spotifyTokenService.getValidBearerAccessToken(task.getUserId());
            addTrackUrisToPlaylist(task, newReleaseTrackUris, bearerToken);
            addTrackUrisToAddedTaskItems(task, newReleaseTrackUris);
            updateTaskExecutionTime(task.getId());

            Log.info(String.format("Task executed successfully [taskId=%s, userId=%s, playlistId=%s, addedTracks=%d]",
                    task.getId(), task.getUserId(), task.getPlaylistId(), newReleaseTrackUris.size()));
        } catch (Exception e) {
            Log.error(String.format("Failed to execute task [taskId=%s, userId=%s, playlistId=%s]",
                    task.getId(), task.getUserId(), task.getPlaylistId()), e);
        } finally {
            processingTasks.put(taskId, false);
        }
    }

    public boolean isTaskProcessing(Long taskId) {
        return processingTasks.getOrDefault(taskId, false);
    }

    private void addTrackUrisToPlaylist(Task task, Set<String> trackUris, String bearerToken)
            throws WebApplicationException {
        for (int i = 0; i < trackUris.size(); i += CHUNK_SIZE) {
            final List<String> idsChunk = trackUris.stream()
                    .skip(i)
                    .limit(CHUNK_SIZE)
                    .toList();
            final PlaylistAddItemsRequest request = new PlaylistAddItemsRequest(idsChunk, null);
            spotifyApi.addToPlaylist(bearerToken, task.getPlaylistId(), request);
        }
    }

    @Transactional
    protected void addTrackUrisToAddedTaskItems(Task task, Set<String> trackUris) {
        Instant now = Instant.now();
        for (String trackUri : trackUris) {
            AddedItem addedItem = new AddedItem();
            addedItem.setTask(task);
            addedItem.setExternalId(trackUri);
            addedItem.setAddedAt(now);
            addedItemRepository.persist(addedItem);
        }
    }

    @Transactional
    protected void updateTaskExecutionTime(long taskId) {
        Task task = taskRepository.findById(taskId);
        task.setLastTimeExecuted(Instant.now());
        taskRepository.persist(task);
    }
}
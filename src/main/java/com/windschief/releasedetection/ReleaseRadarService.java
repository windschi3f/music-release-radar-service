package com.windschief.releasedetection;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import com.windschief.auth.SpotifyTokenService;
import com.windschief.client.HttpClientService;
import com.windschief.spotify.SpotifyApi;
import com.windschief.spotify.model.AlbumItem;
import com.windschief.spotify.model.PlaylistAddItemsRequest;
import com.windschief.spotify.model.TrackItem;
import com.windschief.spotify.model.TracksResponse;
import com.windschief.task.Task;
import com.windschief.task.TaskRepository;
import com.windschief.task.added_item.AddedItem;
import com.windschief.task.added_item.AddedItemRepository;
import com.windschief.task.added_item.AddedItemType;

import io.micrometer.core.annotation.Counted;
import io.micrometer.core.annotation.Timed;
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
    private final HttpClientService httpClientService;

    private final ConcurrentMap<Long, Boolean> processingTasks = new ConcurrentHashMap<>();

    @Inject
    public ReleaseRadarService(
            ReleaseDetectionService releaseDetectionService,
            TaskRepository taskRepository,
            SpotifyTokenService spotifyTokenService,
            @RestClient SpotifyApi spotifyApi,
            AddedItemRepository addedItemRepository,
            HttpClientService httpClientService) {
        this.releaseDetectionService = releaseDetectionService;
        this.taskRepository = taskRepository;
        this.spotifyTokenService = spotifyTokenService;
        this.spotifyApi = spotifyApi;
        this.addedItemRepository = addedItemRepository;
        this.httpClientService = httpClientService;
    }

    @Counted(value = "release.radar.jobs", description = "Number of release radar jobs executed")
    @Timed(value = "release.radar.job.duration", description = "Time taken to execute release radar jobs")
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

    @Counted(value = "release.radar.tasks", description = "Number of individual tasks processed")
    @Timed(value = "release.radar.task.duration", description = "Time taken to process individual tasks")
    public void execute(Task task) {
        final long taskId = task.getId();
        processingTasks.put(taskId, true);

        try {
            final List<AlbumItem> newAlbumReleases = releaseDetectionService.detectNewAlbumReleases(taskId);

            int addedTracks = 0;
            if (!newAlbumReleases.isEmpty()) {
                final String token = spotifyTokenService.getValidBearerAccessToken(task.getUserId());
                final List<TrackItem> newTrackReleases = fetchTracksFromAlbums(token, newAlbumReleases);
                addTracksToPlaylist(token, task, newTrackReleases);
                updateAddedTaskItems(task, newAlbumReleases, newTrackReleases);
                addedTracks = newTrackReleases.size();
            }

            Log.info(String.format("Task executed successfully [taskId=%s, userId=%s, playlistId=%s, addedTracks=%d]",
                    task.getId(), task.getUserId(), task.getPlaylistId(), addedTracks));
        } catch (Exception e) {
            Log.error(String.format("Failed to execute task [taskId=%s, userId=%s, playlistId=%s]",
                    task.getId(), task.getUserId(), task.getPlaylistId()), e);
        } finally {
            updateTasksLastExecution(task.getId());
            processingTasks.put(taskId, false);
        }
    }

    public boolean isTaskProcessing(Long taskId) {
        return processingTasks.getOrDefault(taskId, false);
    }

    @Counted(value = "spotify.tracks.fetch")
    @Timed(value = "spotify.tracks.fetch.duration", description = "Time taken to fetch tracks from albums")
    protected List<TrackItem> fetchTracksFromAlbums(String token, List<AlbumItem> newAlbumReleases)
            throws WebApplicationException, IOException, InterruptedException {
        final List<TrackItem> tracks = new ArrayList<>();

        final List<String> albumIds = newAlbumReleases.stream()
                .map(AlbumItem::id)
                .toList();
        for (String albumId : albumIds) {
            TracksResponse response = spotifyApi.getAlbumTracks(token, albumId, 50, 0);
            while (true) {
                tracks.addAll(response.items());
                if (response.next() == null) {
                    break;
                }

                response = httpClientService.get(response.next(), token, TracksResponse.class);
            }
        }

        return tracks;
    }

    @Counted(value = "spotify.playlist.update")
    @Timed(value = "spotify.playlist.update.duration", description = "Time taken to add tracks to playlist")
    protected void addTracksToPlaylist(String token, Task task, List<TrackItem> newTrackReleases)
            throws WebApplicationException {
        final List<String> trackUris = newTrackReleases.stream()
                .map(TrackItem::uri)
                .toList();
        for (int i = 0; i < trackUris.size(); i += CHUNK_SIZE) {
            final List<String> idsChunk = trackUris.stream()
                    .skip(i)
                    .limit(CHUNK_SIZE)
                    .toList();
            final PlaylistAddItemsRequest request = new PlaylistAddItemsRequest(idsChunk, null);
            spotifyApi.addToPlaylist(token, task.getPlaylistId(), request);
        }
    }

    @Transactional
    protected void updateAddedTaskItems(Task task, List<AlbumItem> newAlbumReleases, List<TrackItem> newTrackReleases) {
        Instant now = Instant.now();
        for (AlbumItem album : newAlbumReleases) {
            AddedItem addedItem = new AddedItem();
            addedItem.setTask(task);
            addedItem.setExternalId(album.id());
            addedItem.setItemType(AddedItemType.ALBUM);
            addedItem.setAddedAt(now);
            addedItemRepository.persist(addedItem);
        }

        for (TrackItem track : newTrackReleases) {
            AddedItem addedItem = new AddedItem();
            addedItem.setTask(task);
            addedItem.setExternalId(track.id());
            addedItem.setItemType(AddedItemType.TRACK);
            addedItem.setAddedAt(now);
            addedItemRepository.persist(addedItem);
        }
    }

    @Transactional
    protected void updateTasksLastExecution(long taskId) {
        Task task = taskRepository.findById(taskId);
        task.setLastTimeExecuted(Instant.now());
        taskRepository.persist(task);
    }
}
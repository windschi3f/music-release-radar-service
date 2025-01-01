package com.windschief.releasedetection;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.time.Instant;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.windschief.auth.SpotifyTokenService;
import com.windschief.spotify.SpotifyApi;
import com.windschief.task.Task;
import com.windschief.task.TaskRepository;

import io.quarkus.hibernate.orm.panache.PanacheQuery;
import jakarta.ws.rs.WebApplicationException;

public class ReleaseRadarServiceTest {
    private final ReleaseDetectionService releaseDetectionService = mock(ReleaseDetectionService.class);
    private final TaskRepository taskRepository = mock(TaskRepository.class);
    private final SpotifyTokenService spotifyTokenService = mock(SpotifyTokenService.class);
    private final SpotifyApi spotifyApi = mock(SpotifyApi.class);
    private final ReleaseRadarService releaseRadarService = new ReleaseRadarService(releaseDetectionService,
            taskRepository, spotifyTokenService, spotifyApi);

    @SuppressWarnings("unchecked")
    private final PanacheQuery<Task> panacheTaskQuery = mock(PanacheQuery.class);

    @BeforeEach
    public void setup() {
        when(taskRepository.findAll()).thenReturn(panacheTaskQuery);
    }

    @Test
    public void givenNoTasks_whenAddNewReleases_thenNoAlbumsAreAdded()
            throws WebApplicationException, IOException, InterruptedException {
        // GIVEN
        when(panacheTaskQuery.stream()).thenReturn(Stream.of());

        // WHEN
        releaseRadarService.addNewReleases();

        // THEN
        verify(releaseDetectionService, times(0)).detectNewAlbumIds(any(Task.class));
    }

    @Test
    public void givenInactiveTask_whenAddNewReleases_thenNoAlbumsAreAdded()
            throws WebApplicationException, IOException, InterruptedException {
        // GIVEN
        Task task = new Task();
        task.setActive(false);
        task.setExecutionIntervalDays(1);
        when(panacheTaskQuery.stream()).thenReturn(Stream.of(task));

        // WHEN
        releaseRadarService.addNewReleases();

        // THEN
        verify(releaseDetectionService, times(0)).detectNewAlbumIds(any(Task.class));
    }

    @Test
    public void givenTaskNotDue_whenAddNewReleases_thenNoAlbumsAreAdded()
            throws WebApplicationException, IOException, InterruptedException {
        // GIVEN
        Task task = new Task();
        task.setActive(true);
        task.setExecutionIntervalDays(1);
        task.setLastTimeExecuted(Instant.now());
        when(panacheTaskQuery.stream()).thenReturn(Stream.of(task));

        // WHEN
        releaseRadarService.addNewReleases();

        // THEN
        verify(releaseDetectionService, times(0)).detectNewAlbumIds(any(Task.class));
    }

    @Test
    public void givenTaskDue_whenAddNewReleases_thenAlbumsAreAdded()
            throws WebApplicationException, IOException, InterruptedException {
        // GIVEN
        Task task = new Task();
        task.setActive(true);
        task.setExecutionIntervalDays(1);
        task.setLastTimeExecuted(Instant.now().minusSeconds(60 * 60 * 24));
        task.setExternalDestinationId("playlistId");
        when(panacheTaskQuery.stream()).thenReturn(Stream.of(task));
        when(releaseDetectionService.detectNewAlbumIds(task)).thenReturn(Set.of("album1", "album2"));
        when(spotifyTokenService.getValidToken(task.getUserId())).thenReturn("token");

        // WHEN
        releaseRadarService.addNewReleases();

        // THEN
        verify(releaseDetectionService, times(1)).detectNewAlbumIds(task);
        verify(spotifyApi, times(1)).addToPlaylist(eq("token"), eq(task.getExternalDestinationId()), anyString(),
                eq(null));
    }

    @Test
    public void givenTaskDueWithoutNewReleases_whenAddNewReleases_thenNoAlbumsAreAdded()
            throws WebApplicationException, IOException, InterruptedException {
        // GIVEN
        Task task = new Task();
        task.setActive(true);
        task.setExecutionIntervalDays(1);
        task.setLastTimeExecuted(Instant.now().minusSeconds(60 * 60 * 24));
        task.setExternalDestinationId("playlistId");
        when(panacheTaskQuery.stream()).thenReturn(Stream.of(task));
        when(releaseDetectionService.detectNewAlbumIds(task)).thenReturn(Set.of());
        when(spotifyTokenService.getValidToken(task.getUserId())).thenReturn("token");

        // WHEN
        releaseRadarService.addNewReleases();

        // THEN
        verify(releaseDetectionService, times(1)).detectNewAlbumIds(task);
        verify(spotifyApi, times(0)).addToPlaylist(any(), any(), any(), any());
    }

    @Test
    public void givenTaskThrowsException_whenAddNewReleases_thenOtherTasksAreStillProcessed()
            throws WebApplicationException, IOException, InterruptedException {
        // GIVEN
        Task task1 = new Task();
        task1.setActive(true);
        task1.setExecutionIntervalDays(1);
        task1.setLastTimeExecuted(Instant.now().minusSeconds(60 * 60 * 24));

        Task task2 = new Task();
        task2.setActive(true);
        task2.setExecutionIntervalDays(1);
        task2.setLastTimeExecuted(Instant.now().minusSeconds(60 * 60 * 24));

        when(panacheTaskQuery.stream()).thenReturn(Stream.of(task1, task2));
        when(releaseDetectionService.detectNewAlbumIds(task1)).thenThrow(new WebApplicationException("Test exception"));
        when(releaseDetectionService.detectNewAlbumIds(task2)).thenReturn(Set.of("album1"));
        when(spotifyTokenService.getValidToken(task2.getUserId())).thenReturn("token");

        // WHEN
        releaseRadarService.addNewReleases();

        // THEN
        verify(spotifyApi, times(1)).addToPlaylist("token", task2.getExternalDestinationId(), "album1", null);
    }

    @Test
    public void givenSpotifyApiThrowsException_whenAddNewReleases_thenErrorIsHandled()
            throws WebApplicationException, IOException, InterruptedException {
        // GIVEN
        Task task = new Task();
        task.setActive(true);
        task.setExecutionIntervalDays(1);
        task.setLastTimeExecuted(Instant.now().minusSeconds(60 * 60 * 24));
        task.setExternalDestinationId("playlistId");

        when(panacheTaskQuery.stream()).thenReturn(Stream.of(task));
        when(releaseDetectionService.detectNewAlbumIds(task)).thenReturn(Set.of("album1"));
        when(spotifyTokenService.getValidToken(task.getUserId())).thenReturn("token");

        doThrow(new WebApplicationException("Test exception")).when(spotifyApi).addToPlaylist(any(), any(), any(),
                any());

        // WHEN
        releaseRadarService.addNewReleases();

        // THEN
        verify(spotifyApi).addToPlaylist(any(), any(), any(), any());
        verify(taskRepository, times(0)).persist(any(Task.class));
    }
}

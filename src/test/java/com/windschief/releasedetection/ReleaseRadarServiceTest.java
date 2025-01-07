package com.windschief.releasedetection;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.windschief.auth.SpotifyTokenService;
import com.windschief.spotify.SpotifyApi;
import com.windschief.spotify.model.TrackItem;
import com.windschief.task.Task;
import com.windschief.task.TaskRepository;
import com.windschief.task.added_item.AddedItemRepository;

import io.quarkus.hibernate.orm.panache.PanacheQuery;
import jakarta.ws.rs.WebApplicationException;

public class ReleaseRadarServiceTest {
        private final ReleaseDetectionService releaseDetectionService = mock(ReleaseDetectionService.class);
        private final TaskRepository taskRepository = mock(TaskRepository.class);
        private final SpotifyTokenService spotifyTokenService = mock(SpotifyTokenService.class);
        private final SpotifyApi spotifyApi = mock(SpotifyApi.class);
        private final AddedItemRepository addedItemRepository = mock(AddedItemRepository.class);
        private final ReleaseRadarService releaseRadarService = new ReleaseRadarService(releaseDetectionService,
                        taskRepository, spotifyTokenService, spotifyApi, addedItemRepository);

        @SuppressWarnings("unchecked")
        private final PanacheQuery<Task> panacheTaskQuery = mock(PanacheQuery.class);

        @BeforeEach
        public void setup() {
                when(taskRepository.findAll()).thenReturn(panacheTaskQuery);
        }

        @Test
        public void givenNoTasks_whenAddNewReleases_thenNoTracksAreAdded()
                        throws WebApplicationException, IOException, InterruptedException {
                // GIVEN
                when(panacheTaskQuery.stream()).thenReturn(Stream.of());

                // WHEN
                releaseRadarService.addNewReleases();

                // THEN
                verify(releaseDetectionService, times(0)).detectNewReleaseTracks(any(Long.class));
        }

        @Test
        public void givenInactiveTask_whenAddNewReleases_thenNoTracksAreAdded()
                        throws WebApplicationException, IOException, InterruptedException {
                // GIVEN
                Task task = new Task();
                task.setActive(false);
                task.setExecutionIntervalDays(1);
                when(panacheTaskQuery.stream()).thenReturn(Stream.of(task));

                // WHEN
                releaseRadarService.addNewReleases();

                // THEN
                verify(releaseDetectionService, times(0)).detectNewReleaseTracks(any(Long.class));
        }

        @Test
        public void givenTaskNotDue_whenAddNewReleases_thenNoTracksAreAdded()
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
                verify(releaseDetectionService, times(0)).detectNewReleaseTracks(any(Long.class));
        }

        @Test
        public void givenTaskDue_whenAddNewReleases_thenTracksAreAdded()
                        throws WebApplicationException, IOException, InterruptedException {
                // GIVEN
                Task task = new Task();
                task.setId(1L);
                task.setActive(true);
                task.setExecutionIntervalDays(1);
                task.setLastTimeExecuted(Instant.now().minusSeconds(60 * 60 * 24));
                task.setExternalDestinationId("playlistId");
                when(panacheTaskQuery.stream()).thenReturn(Stream.of(task));

                TrackItem track1 = createTrackItem("spotify:track:track1");
                TrackItem track2 = createTrackItem("spotify:track:track2");
                when(releaseDetectionService.detectNewReleaseTracks(task.getId())).thenReturn(List.of(track1, track2));
                when(spotifyTokenService.getValidBearerAccessToken(task.getUserId())).thenReturn("token");

                // WHEN
                releaseRadarService.addNewReleases();

                // THEN
                verify(releaseDetectionService, times(1)).detectNewReleaseTracks(task.getId());
                verify(spotifyApi, times(1)).addToPlaylist(any(), any(), any());
        }

        @Test
        public void givenTaskDueWithoutNewReleases_whenAddNewReleases_thenNoTracksAreAdded()
                        throws WebApplicationException, IOException, InterruptedException {
                // GIVEN
                Task task = new Task();
                task.setId(1L);
                task.setActive(true);
                task.setExecutionIntervalDays(1);
                task.setLastTimeExecuted(Instant.now().minusSeconds(60 * 60 * 24));
                task.setExternalDestinationId("playlistId");
                when(panacheTaskQuery.stream()).thenReturn(Stream.of(task));
                when(releaseDetectionService.detectNewReleaseTracks(task.getId())).thenReturn(List.of());
                when(spotifyTokenService.getValidBearerAccessToken(task.getUserId())).thenReturn("token");

                // WHEN
                releaseRadarService.addNewReleases();

                // THEN
                verify(releaseDetectionService, times(1)).detectNewReleaseTracks(task.getId());
                verify(spotifyApi, times(0)).addToPlaylist(any(), any(), any());
        }

        @Test
        public void givenTaskThrowsException_whenAddNewReleases_thenOtherTasksAreStillProcessed()
                        throws WebApplicationException, IOException, InterruptedException {
                // GIVEN
                Task task1 = new Task();
                task1.setId(1L);
                task1.setActive(true);
                task1.setExecutionIntervalDays(1);
                task1.setLastTimeExecuted(Instant.now().minusSeconds(60 * 60 * 24));

                Task task2 = new Task();
                task2.setId(2L);
                task2.setActive(true);
                task2.setExecutionIntervalDays(1);
                task2.setLastTimeExecuted(Instant.now().minusSeconds(60 * 60 * 24));

                TrackItem track = createTrackItem("spotify:track:track1");

                when(panacheTaskQuery.stream()).thenReturn(Stream.of(task1, task2));
                when(releaseDetectionService.detectNewReleaseTracks(task1.getId()))
                                .thenThrow(new WebApplicationException("Test exception"));
                when(releaseDetectionService.detectNewReleaseTracks(task2.getId()))
                                .thenReturn(List.of(track));
                when(spotifyTokenService.getValidBearerAccessToken(task2.getUserId())).thenReturn("token");

                // WHEN
                releaseRadarService.addNewReleases();

                // THEN
                verify(spotifyApi, times(1)).addToPlaylist(any(), any(), any());
        }

        @Test
        public void givenSpotifyApiThrowsException_whenAddNewReleases_thenErrorIsHandled()
                        throws WebApplicationException, IOException, InterruptedException {
                // GIVEN
                Task task = new Task();
                task.setId(1L);
                task.setActive(true);
                task.setExecutionIntervalDays(1);
                task.setLastTimeExecuted(Instant.now().minusSeconds(60 * 60 * 24));
                task.setExternalDestinationId("playlistId");

                TrackItem track = createTrackItem("spotify:track:track1");

                when(panacheTaskQuery.stream()).thenReturn(Stream.of(task));
                when(releaseDetectionService.detectNewReleaseTracks(task.getId())).thenReturn(List.of(track));
                when(spotifyTokenService.getValidBearerAccessToken(task.getUserId())).thenReturn("token");

                doThrow(new WebApplicationException("Test exception"))
                                .when(spotifyApi).addToPlaylist(any(), any(), any());

                // WHEN
                releaseRadarService.addNewReleases();

                // THEN
                verify(spotifyApi).addToPlaylist(any(), any(), any());
                verify(taskRepository, times(0)).persist(any(Task.class));
        }

        TrackItem createTrackItem(String uri) {
                return new TrackItem(
                                null,
                                null,
                                null,
                                0,
                                0,
                                false,
                                null,
                                null,
                                null,
                                null,
                                false,
                                null,
                                null,
                                null,
                                0,
                                null,
                                0,
                                null,
                                uri,
                                false);
        }
}
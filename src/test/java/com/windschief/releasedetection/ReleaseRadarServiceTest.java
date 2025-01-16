package com.windschief.releasedetection;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.windschief.auth.SpotifyTokenException;
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

import io.quarkus.hibernate.orm.panache.PanacheQuery;
import jakarta.ws.rs.WebApplicationException;

public class ReleaseRadarServiceTest {
        private static final String ACCESS_TOKEN = "accessToken";

        private final ReleaseDetectionService releaseDetectionService = mock(ReleaseDetectionService.class);
        private final TaskRepository taskRepository = mock(TaskRepository.class);
        private final SpotifyTokenService spotifyTokenService = mock(SpotifyTokenService.class);
        private final SpotifyApi spotifyApi = mock(SpotifyApi.class);
        private final AddedItemRepository addedItemRepository = mock(AddedItemRepository.class);
        private final HttpClientService httpClientService = mock(HttpClientService.class);
        private final ReleaseRadarService releaseRadarService = new ReleaseRadarService(releaseDetectionService,
                        taskRepository, spotifyTokenService, spotifyApi, addedItemRepository, httpClientService);

        @SuppressWarnings("unchecked")
        private final PanacheQuery<Task> panacheTaskQuery = mock(PanacheQuery.class);

        @BeforeEach
        public void setup() throws WebApplicationException, SpotifyTokenException {
                when(spotifyTokenService.getValidBearerAccessToken(any())).thenReturn(ACCESS_TOKEN);
                when(taskRepository.findAll()).thenReturn(panacheTaskQuery);
        }

        @Test
        public void givenNoTasks_whenAddNewReleases_thenNoTracksAreAdded()
                        throws WebApplicationException, IOException, InterruptedException, SpotifyTokenException {
                // GIVEN
                when(panacheTaskQuery.stream()).thenReturn(Stream.of());

                // WHEN
                releaseRadarService.addNewReleases();

                // THEN
                verify(releaseDetectionService, times(0)).detectNewAlbumReleases(any(Long.class));
        }

        @Test
        public void givenInactiveTask_whenAddNewReleases_thenNoTracksAreAdded()
                        throws WebApplicationException, IOException, InterruptedException, SpotifyTokenException {
                // GIVEN
                Task task = new Task();
                task.setActive(false);
                task.setExecutionIntervalDays(1);
                when(panacheTaskQuery.stream()).thenReturn(Stream.of(task));

                // WHEN
                releaseRadarService.addNewReleases();

                // THEN
                verify(releaseDetectionService, times(0)).detectNewAlbumReleases(any(Long.class));
        }

        @Test
        public void givenTaskNotDue_whenAddNewReleases_thenNoTracksAreAdded()
                        throws WebApplicationException, IOException, InterruptedException, SpotifyTokenException {
                // GIVEN
                Task task = new Task();
                task.setActive(true);
                task.setExecutionIntervalDays(1);
                task.setLastTimeExecuted(Instant.now());
                when(panacheTaskQuery.stream()).thenReturn(Stream.of(task));

                // WHEN
                releaseRadarService.addNewReleases();

                // THEN
                verify(releaseDetectionService, times(0)).detectNewAlbumReleases(any(Long.class));
        }

        @Test
        public void givenTaskDueAndNewRelease_whenAddNewReleases_thenTracksAreAdded()
                        throws WebApplicationException, IOException, InterruptedException, SpotifyTokenException {
                // GIVEN
                Task task = new Task();
                task.setId(1L);
                task.setActive(true);
                task.setExecutionIntervalDays(1);
                task.setLastTimeExecuted(Instant.now().minusSeconds(60 * 60 * 24));
                task.setPlaylistId("playlistId");
                when(panacheTaskQuery.stream()).thenReturn(Stream.of(task));
                when(taskRepository.findById(task.getId())).thenReturn(task);

                AlbumItem album = createAlbumItem("album1");
                when(releaseDetectionService.detectNewAlbumReleases(task.getId())).thenReturn(List.of(album));

                TrackItem track = createTrackItem("track1", "spotify:track:track1");
                TracksResponse tracksResponse = new TracksResponse(null, 20, null, 0, null, 1, List.of(track));
                when(spotifyApi.getAlbumTracks(ACCESS_TOKEN, album.id(), 50, 0)).thenReturn(tracksResponse);

                // WHEN
                releaseRadarService.addNewReleases();

                // THEN
                verify(releaseDetectionService, times(1)).detectNewAlbumReleases(task.getId());
                verify(spotifyApi, times(1)).addToPlaylist(any(), any(), any());
                verify(addedItemRepository, times(2)).persist(any(AddedItem.class));
                verify(taskRepository, times(1)).persist(any(Task.class));
        }

        @Test
        public void givenTaskDueWithoutNewReleases_whenAddNewReleases_thenNoTracksAreAdded()
                        throws WebApplicationException, IOException, InterruptedException, SpotifyTokenException {
                // GIVEN
                Task task = new Task();
                task.setId(1L);
                task.setActive(true);
                task.setExecutionIntervalDays(1);
                task.setLastTimeExecuted(Instant.now().minusSeconds(60 * 60 * 24));
                task.setPlaylistId("playlistId");
                when(panacheTaskQuery.stream()).thenReturn(Stream.of(task));
                when(taskRepository.findById(task.getId())).thenReturn(task);
                when(releaseDetectionService.detectNewAlbumReleases(task.getId())).thenReturn(List.of());

                // WHEN
                releaseRadarService.addNewReleases();

                // THEN
                verify(releaseDetectionService, times(1)).detectNewAlbumReleases(task.getId());
                verify(spotifyApi, times(0)).addToPlaylist(any(), any(), any());
        }

        @Test
        public void givenMultiplePages_whenAddNewReleases_thenAllTracksAreAdded()
                        throws WebApplicationException, IOException, InterruptedException, SpotifyTokenException {
                // GIVEN
                Task task = new Task();
                task.setId(1L);
                task.setActive(true);
                task.setExecutionIntervalDays(1);
                task.setLastTimeExecuted(Instant.now().minusSeconds(60 * 60 * 24));
                task.setPlaylistId("playlistId");
                when(panacheTaskQuery.stream()).thenReturn(Stream.of(task));
                when(taskRepository.findById(task.getId())).thenReturn(task);
                when(releaseDetectionService.detectNewAlbumReleases(task.getId()))
                                .thenReturn(List.of(createAlbumItem("album1")));

                TrackItem track1 = createTrackItem("track1", "spotify:track:track1");
                TracksResponse tracksResponse1 = new TracksResponse(null, 20, "nextPage", 0, null, 2, List.of(track1));
                when(spotifyApi.getAlbumTracks(eq(ACCESS_TOKEN), anyString(), eq(50), eq(0)))
                                .thenReturn(tracksResponse1);

                TrackItem track2 = createTrackItem("track2", "spotify:track:track2");
                TracksResponse tracksResponse2 = new TracksResponse(null, 20, null, 0, null, 2, List.of(track2));
                when(httpClientService.get(any(), any(), eq(TracksResponse.class))).thenReturn(tracksResponse2);

                // WHEN
                releaseRadarService.addNewReleases();

                // THEN
                verify(spotifyApi, times(1)).addToPlaylist(
                                ACCESS_TOKEN,
                                task.getPlaylistId(),
                                new PlaylistAddItemsRequest(List.of("spotify:track:track1", "spotify:track:track2"),
                                                null));
        }

        @Test
        public void givenTaskThrowsException_whenAddNewReleases_thenOtherTasksAreStillProcessed()
                        throws WebApplicationException, IOException, InterruptedException, SpotifyTokenException {
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
                when(panacheTaskQuery.stream()).thenReturn(Stream.of(task1, task2));
                when(taskRepository.findById(task1.getId())).thenReturn(task1);
                when(taskRepository.findById(task2.getId())).thenReturn(task2);

                when(releaseDetectionService.detectNewAlbumReleases(task1.getId()))
                                .thenThrow(new WebApplicationException("Test exception"));

                AlbumItem album = createAlbumItem("album1");
                when(releaseDetectionService.detectNewAlbumReleases(task2.getId()))
                                .thenReturn(List.of(album));
                TrackItem track = createTrackItem("track1", "spotify:track:track1");
                TracksResponse tracksResponse = new TracksResponse(null, 20, null, 0, null, 1, List.of(track));
                when(spotifyApi.getAlbumTracks(ACCESS_TOKEN, album.id(), 50, 0)).thenReturn(tracksResponse);

                // WHEN
                releaseRadarService.addNewReleases();

                // THEN
                verify(spotifyApi, times(1)).addToPlaylist(any(), any(), any());
        }

        TrackItem createTrackItem(String id, String uri) {
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
                                id,
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

        AlbumItem createAlbumItem(String id) {
                return new AlbumItem(
                                null,
                                0,
                                null,
                                null,
                                null,
                                "album1",
                                null,
                                null,
                                "2024-12-24",
                                "day",
                                null,
                                null,
                                null,
                                null, null);
        }
}
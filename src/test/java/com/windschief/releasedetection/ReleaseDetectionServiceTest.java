package com.windschief.releasedetection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.windschief.auth.SpotifyTokenService;
import com.windschief.client.HttpClientService;
import com.windschief.spotify.SpotifyApi;
import com.windschief.spotify.model.AlbumItem;
import com.windschief.spotify.model.AlbumsResponse;
import com.windschief.spotify.model.TrackItem;
import com.windschief.spotify.model.TracksResponse;
import com.windschief.task.Platform;
import com.windschief.task.Task;
import com.windschief.task.TaskRepository;
import com.windschief.task.added_item.AddedItemRepository;
import com.windschief.task.item.TaskItem;
import com.windschief.task.item.TaskItemType;

import jakarta.ws.rs.WebApplicationException;

public class ReleaseDetectionServiceTest {
        private static final String ACCESS_TOKEN = "accessToken";

        private final SpotifyApi spotifyApi = mock(SpotifyApi.class);
        private final AddedItemRepository addedItemRepository = mock(AddedItemRepository.class);
        private final SpotifyTokenService spotifyTokenService = mock(SpotifyTokenService.class);
        private final HttpClientService httpClientService = mock(HttpClientService.class);
        private final TaskRepository taskRepository = mock(TaskRepository.class);
        private final ReleaseDetectionService releaseDetectionService = new ReleaseDetectionService(spotifyApi,
                        addedItemRepository, spotifyTokenService, httpClientService, taskRepository);

        @BeforeEach
        void setup() {
                when(spotifyTokenService.getValidBearerAccessToken("userId")).thenReturn(ACCESS_TOKEN);
        }

        @Test
        void givenUnspportedPlatform_whenDetectNewReleaseTracks_thenThrowIllegalArgumentException() {
                // GIVEN
                Task task = mock(Task.class);
                when(task.getPlatform()).thenReturn(Platform.YOUTUBE);
                when(taskRepository.findById(1L)).thenReturn(task);

                // WHEN
                IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
                        releaseDetectionService.detectNewReleaseTracks(1L);
                });

                // THEN
                assertEquals("Unsupported platform: YOUTUBE", exception.getMessage());
        }

        @Test
        void givenUnspportedTaskItemType_whenDetectNewReleaseTracks_thenThrowIllegalArgumentException() {
                // GIVEN
                Task task = mock(Task.class);
                when(task.getPlatform()).thenReturn(Platform.SPOTIFY);
                TaskItem taskItem = mock(TaskItem.class);
                when(taskItem.getItemType()).thenReturn(TaskItemType.PLAYLIST);
                when(task.getTaskItems()).thenReturn(List.of(taskItem));
                when(taskRepository.findById(1L)).thenReturn(task);

                // WHEN
                IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
                        releaseDetectionService.detectNewReleaseTracks(1L);
                });

                // THEN
                assertEquals("Unsupported task item type: PLAYLIST", exception.getMessage());
        }

        @Test
        void givenNewRelease_whenDetectNewReleaseTracks_thenDetectNewReleases()
                        throws WebApplicationException, IOException, InterruptedException {
                // GIVEN
                Task task = mock(Task.class);
                when(task.getPlatform()).thenReturn(Platform.SPOTIFY);
                TaskItem taskItem = mock(TaskItem.class);
                when(taskItem.getItemType()).thenReturn(TaskItemType.ARTIST);
                when(taskItem.getExternalReferenceId()).thenReturn("artistId");
                when(task.getTaskItems()).thenReturn(List.of(taskItem));
                when(task.getUserId()).thenReturn("userId");
                when(task.getId()).thenReturn(1L);
                when(task.getCheckFrom()).thenReturn(Instant.parse("2024-12-25T00:00:00Z"));
                when(taskRepository.findById(1L)).thenReturn(task);

                when(addedItemRepository.existsByExternalIdAndTaskId(any(), any())).thenReturn(false);

                AlbumItem albumItem = new AlbumItem(null, 0, null, null, null, "albumId",
                                null, null, "2024-12-26", "day", null, null, null, null, null);
                AlbumsResponse albumsResponse = new AlbumsResponse(
                                null,
                                20,
                                null,
                                0,
                                null,
                                1,
                                List.of(albumItem));
                when(spotifyApi.getArtistAlbums(ACCESS_TOKEN, "artistId", "album,single", 50, 0))
                                .thenReturn(albumsResponse);

                TrackItem track = createTrackItem("spotify:track:track1");
                TracksResponse tracksResponse = new TracksResponse(null, 20, null, 0, null, 1, List.of(track));
                when(spotifyApi.getAlbumTracks(ACCESS_TOKEN, "albumId", 50, 0)).thenReturn(tracksResponse);

                // WHEN
                List<TrackItem> trackItems = releaseDetectionService.detectNewReleaseTracks(1L);

                // THEN
                assertEquals(1, trackItems.size());
                assertTrue(trackItems.contains(track));
        }

        @Test
        void givenAlreadyAddedNewRelease_whenDetectNewReleaseTracks_thenDetectNoNewReleases()
                        throws WebApplicationException, IOException, InterruptedException {
                // GIVEN
                Task task = mock(Task.class);
                when(task.getPlatform()).thenReturn(Platform.SPOTIFY);
                TaskItem taskItem = mock(TaskItem.class);
                when(taskItem.getItemType()).thenReturn(TaskItemType.ARTIST);
                when(taskItem.getExternalReferenceId()).thenReturn("artistId");
                when(task.getTaskItems()).thenReturn(List.of(taskItem));
                when(task.getUserId()).thenReturn("userId");
                when(task.getId()).thenReturn(1L);
                when(task.getCheckFrom()).thenReturn(Instant.parse("2024-12-25T00:00:00Z"));
                when(taskRepository.findById(1L)).thenReturn(task);

                when(addedItemRepository.existsByExternalIdAndTaskId(any(), any())).thenReturn(true);

                AlbumItem albumItem = new AlbumItem(null, 0, null, null, null, "albumId",
                                null, null, "2024-12-26", "day", null, null, null, null, null);
                AlbumsResponse albumsResponse = new AlbumsResponse(
                                null,
                                20,
                                null,
                                0,
                                null,
                                1,
                                List.of(albumItem));
                when(spotifyApi.getArtistAlbums(ACCESS_TOKEN, "artistId", "album,single", 50, 0))
                                .thenReturn(albumsResponse);

                TrackItem track = createTrackItem("spotify:track:track1");
                TracksResponse tracksResponse = new TracksResponse(null, 20, null, 0, null, 1, List.of(track));
                when(spotifyApi.getAlbumTracks(ACCESS_TOKEN, "albumId", 50, 0)).thenReturn(tracksResponse);

                // WHEN
                List<TrackItem> trackItems = releaseDetectionService.detectNewReleaseTracks(1L);

                // THEN
                assertEquals(0, trackItems.size());
        }

        @Test
        void givenReleaseBeforeCheckFrom_whenDetectNewReleaseTracks_thenDetectNoNewReleases()
                        throws WebApplicationException, IOException, InterruptedException {
                // GIVEN
                Task task = mock(Task.class);
                when(task.getPlatform()).thenReturn(Platform.SPOTIFY);
                TaskItem taskItem = mock(TaskItem.class);
                when(taskItem.getItemType()).thenReturn(TaskItemType.ARTIST);
                when(taskItem.getExternalReferenceId()).thenReturn("artistId");
                when(task.getTaskItems()).thenReturn(List.of(taskItem));
                when(task.getUserId()).thenReturn("userId");
                when(task.getId()).thenReturn(1L);
                when(task.getCheckFrom()).thenReturn(Instant.parse("2024-12-26T00:00:00Z"));
                when(taskRepository.findById(1L)).thenReturn(task);

                when(addedItemRepository.existsByExternalIdAndTaskId(any(), any())).thenReturn(false);

                AlbumItem albumItem = new AlbumItem(null, 0, null, null, null, "albumId",
                                null, null, "2024-12-24", "day", null, null, null, null, null);

                AlbumsResponse albumsResponse = new AlbumsResponse(
                                null,
                                20,
                                null,
                                0,
                                null,
                                1,
                                List.of(albumItem));

                when(spotifyApi.getArtistAlbums(ACCESS_TOKEN, "artistId", "album,single", 50, 0))
                                .thenReturn(albumsResponse);

                // WHEN
                List<TrackItem> trackItems = releaseDetectionService.detectNewReleaseTracks(1L);

                // THEN
                assertEquals(0, trackItems.size());
        }

        @Test
        void givenMultiplePages_whenDetectNewReleaseTracks_thenDetectNewReleases()
                        throws WebApplicationException, IOException, InterruptedException {
                // GIVEN
                Task task = mock(Task.class);
                when(task.getPlatform()).thenReturn(Platform.SPOTIFY);
                TaskItem taskItem = mock(TaskItem.class);
                when(taskItem.getItemType()).thenReturn(TaskItemType.ARTIST);
                when(taskItem.getExternalReferenceId()).thenReturn("artistId");
                when(task.getTaskItems()).thenReturn(List.of(taskItem));
                when(task.getUserId()).thenReturn("userId");
                when(task.getId()).thenReturn(1L);
                when(task.getCheckFrom()).thenReturn(Instant.parse("2024-12-24T00:00:00Z"));
                when(addedItemRepository.existsByExternalIdAndTaskId(any(), any())).thenReturn(false);
                when(taskRepository.findById(1L)).thenReturn(task);

                AlbumItem album1 = new AlbumItem(null, 0, null, null, null, "album1", null, null, "2024-12-24", "day",
                                null,
                                null, null, null, null);
                AlbumsResponse response1 = new AlbumsResponse(null, 20, "nextPage", 0, null, 2, List.of(album1));
                AlbumItem album2 = new AlbumItem(null, 0, null, null, null, "album2", null, null, "2024-12-25", "day",
                                null,
                                null, null, null, null);
                AlbumsResponse response2 = new AlbumsResponse(null, 20, null, 0, null, 2, List.of(album2));
                when(spotifyApi.getArtistAlbums(ACCESS_TOKEN, "artistId", "album,single", 50, 0))
                                .thenReturn(response1);
                when(httpClientService.get(any(), any(), eq(AlbumsResponse.class))).thenReturn(response2);

                TrackItem track1 = createTrackItem("spotify:track:track1");
                TracksResponse tracksResponse1 = new TracksResponse(null, 20, "nextPage", 0, null, 2,
                                List.of(track1));
                TrackItem track2 = createTrackItem("spotify:track:track2");
                TracksResponse tracksResponse2 = new TracksResponse(null, 20, null, 0, null, 2, List.of(track2));
                when(spotifyApi.getAlbumTracks(eq(ACCESS_TOKEN), anyString(), eq(50), eq(0)))
                                .thenReturn(tracksResponse1);
                when(httpClientService.get(any(), any(), eq(TracksResponse.class))).thenReturn(tracksResponse2);

                // WHEN
                List<TrackItem> trackItems = releaseDetectionService.detectNewReleaseTracks(1L);

                // THEN
                assertEquals(4, trackItems.size());
                assertTrue(trackItems.contains(track1));
                assertTrue(trackItems.contains(track2));
        }

        @Test
        void givenDifferentDatePrecisions_whenDetectNewReleaseTracks_thenHandleCorrectly()
                        throws WebApplicationException, IOException, InterruptedException {
                // GIVEN
                Task task = mock(Task.class);
                when(task.getPlatform()).thenReturn(Platform.SPOTIFY);
                TaskItem taskItem = mock(TaskItem.class);
                when(taskItem.getItemType()).thenReturn(TaskItemType.ARTIST);
                when(taskItem.getExternalReferenceId()).thenReturn("artistId");
                when(task.getTaskItems()).thenReturn(List.of(taskItem));
                when(task.getUserId()).thenReturn("userId");
                when(task.getId()).thenReturn(1L);
                when(task.getCheckFrom()).thenReturn(Instant.parse("2024-01-02T00:00:00Z"));
                when(taskRepository.findById(1L)).thenReturn(task);

                when(addedItemRepository.existsByExternalIdAndTaskId(any(), any())).thenReturn(false);

                AlbumItem monthAlbum = new AlbumItem(null, 0, null, null, null, "monthAlbum", null, null, "2024-02",
                                "month",
                                null, null, null, null, null);
                AlbumItem yearAlbum = new AlbumItem(null, 0, null, null, null, "yearAlbum", null, null, "2024", "year",
                                null,
                                null, null, null, null);
                AlbumsResponse response = new AlbumsResponse(null, 20, null, 0, null, 2,
                                List.of(monthAlbum, yearAlbum));
                when(spotifyApi.getArtistAlbums(ACCESS_TOKEN, "artistId", "album,single", 50, 0)).thenReturn(response);

                TrackItem track = createTrackItem("spotify:track:track1");
                TracksResponse tracksResponse = new TracksResponse(null, 20, null, 0, null, 1, List.of(track));
                when(spotifyApi.getAlbumTracks(ACCESS_TOKEN, "monthAlbum", 50, 0)).thenReturn(tracksResponse);

                // WHEN
                List<TrackItem> trackItems = releaseDetectionService.detectNewReleaseTracks(1L);

                // THEN
                assertEquals(1, trackItems.size());
                assertTrue(trackItems.contains(track));
        }

        @Test
        void givenUnknownDatePrecision_whenDetectNewReleaseTracks_thenThrowException()
                        throws WebApplicationException {
                // GIVEN
                Task task = mock(Task.class);
                when(task.getPlatform()).thenReturn(Platform.SPOTIFY);
                TaskItem taskItem = mock(TaskItem.class);
                when(taskItem.getItemType()).thenReturn(TaskItemType.ARTIST);
                when(taskItem.getExternalReferenceId()).thenReturn("artistId");
                when(task.getTaskItems()).thenReturn(List.of(taskItem));
                when(task.getUserId()).thenReturn("userId");
                when(task.getId()).thenReturn(1L);
                when(task.getCheckFrom()).thenReturn(Instant.parse("2024-01-01T00:00:00Z"));
                when(taskRepository.findById(1L)).thenReturn(task);

                AlbumItem album = new AlbumItem(null, 0, null, null, null, "albumId", null, null, "2024", "unknown",
                                null, null,
                                null, null, null);
                AlbumsResponse response = new AlbumsResponse(null, 20, null, 0, null, 1, List.of(album));

                when(spotifyApi.getArtistAlbums("accessToken", "artistId", "album,single", 50, 0)).thenReturn(response);
                when(addedItemRepository.existsByExternalIdAndTaskId(any(), any())).thenReturn(false);

                // WHEN
                IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
                        releaseDetectionService.detectNewReleaseTracks(1L);
                });

                // THEN
                assertEquals("Unknown date precision: unknown", exception.getMessage());
        }

        public static TrackItem createTrackItem(String uri) {
                return new TrackItem(
                                null,
                                null,
                                List.of("US"),
                                1,
                                180000,
                                false,
                                null,
                                null,
                                "test-href",
                                "test-id",
                                true,
                                null,
                                null,
                                "Test Track",
                                50,
                                "preview-url",
                                1,
                                "track",
                                uri,
                                false);
        }
}
package com.windschief.releasedetection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.windschief.auth.SpotifyTokenValidator;
import com.windschief.spotify.SpotifyApi;
import com.windschief.spotify.model.AlbumItem;
import com.windschief.spotify.model.AlbumsResponse;
import com.windschief.task.Platform;
import com.windschief.task.Task;
import com.windschief.task.added_item.AddedItemRepository;
import com.windschief.task.item.TaskItem;
import com.windschief.task.item.TaskItemType;

public class ReleaseDetectionServiceTest {

    private final SpotifyApi spotifyApi = mock(SpotifyApi.class);
    private final AddedItemRepository addedItemRepository = mock(AddedItemRepository.class);
    private final SpotifyTokenValidator spotifyTokenValidator = mock(SpotifyTokenValidator.class);
    private final ReleaseDetectionService releaseDetectionService = new ReleaseDetectionService(spotifyApi,
            addedItemRepository, spotifyTokenValidator);

    @Test
    public void givenUnspportedPlatform_whenDetectNewAlbumIds_thenThrowIllegalArgumentException() {
        // GIVEN
        Task task = mock(Task.class);
        when(task.getPlatform()).thenReturn(Platform.YOUTUBE);

        // WHEN
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            releaseDetectionService.detectNewAlbumIds(task);
        });

        // THEN
        assertEquals("Unsupported platform: YOUTUBE", exception.getMessage());
    }

    @Test
    public void givenUnspportedTaskItemType_whenDetectNewAlbumIds_thenThrowIllegalArgumentException() {
        // GIVEN
        Task task = mock(Task.class);
        when(task.getPlatform()).thenReturn(Platform.SPOTIFY);
        TaskItem taskItem = mock(TaskItem.class);
        when(taskItem.getItemType()).thenReturn(TaskItemType.PLAYLIST);
        when(task.getTaskItems()).thenReturn(List.of(taskItem));

        // WHEN
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            releaseDetectionService.detectNewAlbumIds(task);
        });

        // THEN
        assertEquals("Unsupported task item type: PLAYLIST", exception.getMessage());
    }

    @Test
    public void givenNewRelease_whenDetectNewAlbumIds_thenReturnAlbumIds() {
        // GIVEN
        Task task = mock(Task.class);
        when(task.getPlatform()).thenReturn(Platform.SPOTIFY);
        TaskItem taskItem = mock(TaskItem.class);
        when(taskItem.getItemType()).thenReturn(TaskItemType.ARTIST);
        when(taskItem.getExternalReferenceId()).thenReturn("artistId");
        when(task.getTaskItems()).thenReturn(List.of(taskItem));
        when(task.getUserId()).thenReturn("userId");
        when(task.getId()).thenReturn(1L);

        String accessToken = "accessToken";
        when(spotifyTokenValidator.getValidTokenForUser("userId")).thenReturn(accessToken);

        Instant lastAddedAt = Instant.parse("2024-12-24T10:21:33.00Z");
        when(addedItemRepository.getLastAddedAt(task.getId())).thenReturn(lastAddedAt);

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

        when(spotifyApi.getArtistAlbums(accessToken, "artistId", "album,single", 50, 0)).thenReturn(albumsResponse);
        when(addedItemRepository.existsByExternalIdAndTaskId(any(), any())).thenReturn(false);

        // WHEN
        Set<String> albumIds = releaseDetectionService.detectNewAlbumIds(task);

        // THEN
        assertEquals(1, albumIds.size());
        assertTrue(albumIds.contains("albumId"));
    }

    @Test
    public void givenAlreadyAddedNewRelease_whenDetectNewAlbumIds_thenDetectNoNewReleases() {
        // GIVEN
        Task task = mock(Task.class);
        when(task.getPlatform()).thenReturn(Platform.SPOTIFY);
        TaskItem taskItem = mock(TaskItem.class);
        when(taskItem.getItemType()).thenReturn(TaskItemType.ARTIST);
        when(taskItem.getExternalReferenceId()).thenReturn("artistId");
        when(task.getTaskItems()).thenReturn(List.of(taskItem));
        when(task.getUserId()).thenReturn("userId");
        when(task.getId()).thenReturn(1L);

        String accessToken = "accessToken";
        when(spotifyTokenValidator.getValidTokenForUser("userId")).thenReturn(accessToken);

        Instant lastAddedAt = Instant.parse("2024-12-24T10:21:33.00Z");
        when(addedItemRepository.getLastAddedAt(task.getId())).thenReturn(lastAddedAt);

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

        when(spotifyApi.getArtistAlbums(accessToken, "artistId", "album,single", 50, 0)).thenReturn(albumsResponse);
        when(addedItemRepository.existsByExternalIdAndTaskId(any(), any())).thenReturn(true);

        // WHEN
        Set<String> albumIds = releaseDetectionService.detectNewAlbumIds(task);

        // THEN
        assertEquals(0, albumIds.size());
    }

    @Test
    public void givenReleaseBeforeLastAddedAt_whenDetectNewAlbumIds_thenReturnAlbumIds() {
        // GIVEN
        Task task = mock(Task.class);
        when(task.getPlatform()).thenReturn(Platform.SPOTIFY);
        TaskItem taskItem = mock(TaskItem.class);
        when(taskItem.getItemType()).thenReturn(TaskItemType.ARTIST);
        when(taskItem.getExternalReferenceId()).thenReturn("artistId");
        when(task.getTaskItems()).thenReturn(List.of(taskItem));
        when(task.getUserId()).thenReturn("userId");
        when(task.getId()).thenReturn(1L);

        String accessToken = "accessToken";
        when(spotifyTokenValidator.getValidTokenForUser("userId")).thenReturn(accessToken);

        Instant lastAddedAt = Instant.parse("2024-12-26T21:34:00.00Z");
        when(addedItemRepository.getLastAddedAt(task.getId())).thenReturn(lastAddedAt);

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

        when(spotifyApi.getArtistAlbums(accessToken, "artistId", "album,single", 50, 0)).thenReturn(albumsResponse);
        when(addedItemRepository.existsByExternalIdAndTaskId(any(), any())).thenReturn(false);

        // WHEN
        Set<String> albumIds = releaseDetectionService.detectNewAlbumIds(task);

        // THEN
        assertEquals(0, albumIds.size());
    }

    @Test
    public void givenNullLastAddedAt_whenDetectNewAlbumIds_thenReturnAllAlbumIds() {
        // GIVEN
        Task task = mock(Task.class);
        when(task.getPlatform()).thenReturn(Platform.SPOTIFY);
        TaskItem taskItem = mock(TaskItem.class);
        when(taskItem.getItemType()).thenReturn(TaskItemType.ARTIST);
        when(taskItem.getExternalReferenceId()).thenReturn("artistId");
        when(task.getTaskItems()).thenReturn(List.of(taskItem));
        when(task.getUserId()).thenReturn("userId");
        when(task.getId()).thenReturn(1L);

        when(spotifyTokenValidator.getValidTokenForUser("userId")).thenReturn("accessToken");
        when(addedItemRepository.getLastAddedAt(task.getId())).thenReturn(null);

        AlbumItem albumItem = new AlbumItem(null, 0, null, null, null, "albumId",
                null, null, "2024-12-24", "day", null, null, null, null, null);
        AlbumsResponse albumsResponse = new AlbumsResponse(null, 20, null, 0, null, 1, List.of(albumItem));
        when(spotifyApi.getArtistAlbums("accessToken", "artistId", "album,single", 50, 0)).thenReturn(albumsResponse);
        when(addedItemRepository.existsByExternalIdAndTaskId(any(), any())).thenReturn(false);

        // WHEN
        Set<String> albumIds = releaseDetectionService.detectNewAlbumIds(task);

        // THEN
        assertEquals(1, albumIds.size());
        assertTrue(albumIds.contains("albumId"));
    }

    @Test
    public void givenMultiplePages_whenDetectNewAlbumIds_thenReturnAllAlbumIds() {
        // GIVEN
        Task task = mock(Task.class);
        when(task.getPlatform()).thenReturn(Platform.SPOTIFY);
        TaskItem taskItem = mock(TaskItem.class);
        when(taskItem.getItemType()).thenReturn(TaskItemType.ARTIST);
        when(taskItem.getExternalReferenceId()).thenReturn("artistId");
        when(task.getTaskItems()).thenReturn(List.of(taskItem));
        when(task.getUserId()).thenReturn("userId");
        when(task.getId()).thenReturn(1L);

        when(spotifyTokenValidator.getValidTokenForUser("userId")).thenReturn("accessToken");
        when(addedItemRepository.getLastAddedAt(task.getId())).thenReturn(null);

        AlbumItem album1 = new AlbumItem(null, 0, null, null, null, "album1", null, null, "2024-12-24", "day", null,
                null, null, null, null);
        AlbumsResponse response1 = new AlbumsResponse(null, 20, "nextPage", 0, null, 2, List.of(album1));
        AlbumItem album2 = new AlbumItem(null, 0, null, null, null, "album2", null, null, "2024-12-25", "day", null,
                null, null, null, null);
        AlbumsResponse response2 = new AlbumsResponse(null, 20, null, 0, null, 2, List.of(album2));

        when(spotifyApi.getArtistAlbums("accessToken", "artistId", "album,single", 50, 0)).thenReturn(response1);
        when(spotifyApi.getNextPage("accessToken", AlbumsResponse.class)).thenReturn(response2);
        when(addedItemRepository.existsByExternalIdAndTaskId(any(), any())).thenReturn(false);

        // WHEN
        Set<String> albumIds = releaseDetectionService.detectNewAlbumIds(task);

        // THEN
        assertEquals(2, albumIds.size());
        assertTrue(albumIds.containsAll(Set.of("album1", "album2")));
    }

    @Test
    public void givenDifferentDatePrecisions_whenDetectNewAlbumIds_thenHandleCorrectly() {
        // GIVEN
        Task task = mock(Task.class);
        when(task.getPlatform()).thenReturn(Platform.SPOTIFY);
        TaskItem taskItem = mock(TaskItem.class);
        when(taskItem.getItemType()).thenReturn(TaskItemType.ARTIST);
        when(taskItem.getExternalReferenceId()).thenReturn("artistId");
        when(task.getTaskItems()).thenReturn(List.of(taskItem));
        when(task.getUserId()).thenReturn("userId");
        when(task.getId()).thenReturn(1L);

        when(spotifyTokenValidator.getValidTokenForUser("userId")).thenReturn("accessToken");
        Instant lastAddedAt = Instant.parse("2024-01-01T00:00:00Z");
        when(addedItemRepository.getLastAddedAt(task.getId())).thenReturn(lastAddedAt);

        AlbumItem monthAlbum = new AlbumItem(null, 0, null, null, null, "monthAlbum", null, null, "2024-02", "month",
                null, null, null, null, null);
        AlbumItem yearAlbum = new AlbumItem(null, 0, null, null, null, "yearAlbum", null, null, "2024", "year", null,
                null, null, null, null);
        AlbumsResponse response = new AlbumsResponse(null, 20, null, 0, null, 2, List.of(monthAlbum, yearAlbum));

        when(spotifyApi.getArtistAlbums("accessToken", "artistId", "album,single", 50, 0)).thenReturn(response);
        when(addedItemRepository.existsByExternalIdAndTaskId(any(), any())).thenReturn(false);

        // WHEN
        Set<String> albumIds = releaseDetectionService.detectNewAlbumIds(task);

        // THEN
        assertEquals(1, albumIds.size());
        assertTrue(albumIds.contains("monthAlbum"));
    }

    @Test
    public void givenUnknownDatePrecision_whenDetectNewAlbumIds_thenThrowException() {
        // GIVEN
        Task task = mock(Task.class);
        when(task.getPlatform()).thenReturn(Platform.SPOTIFY);
        TaskItem taskItem = mock(TaskItem.class);
        when(taskItem.getItemType()).thenReturn(TaskItemType.ARTIST);
        when(taskItem.getExternalReferenceId()).thenReturn("artistId");
        when(task.getTaskItems()).thenReturn(List.of(taskItem));
        when(task.getUserId()).thenReturn("userId");
        when(task.getId()).thenReturn(1L);

        when(spotifyTokenValidator.getValidTokenForUser("userId")).thenReturn("accessToken");
        Instant lastAddedAt = Instant.parse("2000-01-01T00:00:00Z"); // Set old date to ensure album passes filter
        when(addedItemRepository.getLastAddedAt(task.getId())).thenReturn(lastAddedAt);

        AlbumItem album = new AlbumItem(null, 0, null, null, null, "albumId", null, null, "2024", "unknown", null, null,
                null, null, null);
        AlbumsResponse response = new AlbumsResponse(null, 20, null, 0, null, 1, List.of(album));

        when(spotifyApi.getArtistAlbums("accessToken", "artistId", "album,single", 50, 0)).thenReturn(response);
        when(addedItemRepository.existsByExternalIdAndTaskId(any(), any())).thenReturn(false);

        // WHEN
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            releaseDetectionService.detectNewAlbumIds(task);
        });

        // THEN
        assertEquals("Unknown date precision: unknown", exception.getMessage());
    }

}
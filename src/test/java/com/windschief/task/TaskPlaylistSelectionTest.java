package com.windschief.task;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.windschief.releasedetection.ReleaseRadarService;

import jakarta.ws.rs.BadRequestException;

class TaskPlaylistSelectionTest {
    @Test
    void storesBothPlaylistsAndReturnsThemToTheEditor() {
        TaskRequestDto request = new TaskRequestDto(
                "Discoveries", Platform.SPOTIFY, 7, LocalDate.of(2026, 10, 30),
                true, "discovery", "favorites", TasteSource.PLAYLIST);
        Task task = TaskRequestDto.toTask(request);
        task.setId(42L);

        TaskResponseDto response = new TaskMapper(mock(ReleaseRadarService.class)).toDto(task);

        assertEquals("discovery", task.getPlaylistId());
        assertEquals("favorites", task.getFavoritePlaylistId());
        assertEquals("discovery", response.playlistId());
        assertEquals("favorites", response.favoritePlaylistId());
        assertEquals(TasteSource.PLAYLIST, response.tasteSource());
    }

    @Test
    void changesBothPlaylistsWithoutReactivatingAPausedTask() {
        Task task = new Task();
        task.setPlaylistId("old-discovery");
        task.setFavoritePlaylistId("old-favorites");
        TaskRequestDto request = new TaskRequestDto(
                "Updated", Platform.SPOTIFY, 7, LocalDate.of(2026, 10, 30),
                false, "new-discovery", "new-favorites", TasteSource.PLAYLIST);

        TaskRequestDto.updateTask(task, request);

        assertEquals("new-discovery", task.getPlaylistId());
        assertEquals("new-favorites", task.getFavoritePlaylistId());
        assertFalse(task.isActive());
    }

    @Test
    void switchesFromAPlaylistToLikedSongs() {
        Task task = new Task();
        task.setTasteSource(TasteSource.PLAYLIST);
        task.setFavoritePlaylistId("old-favorites");
        TaskRequestDto request = new TaskRequestDto(
                "Liked Songs", Platform.SPOTIFY, 7, LocalDate.of(2026, 10, 30),
                true, "discovery", null, TasteSource.LIKED_SONGS);

        TaskRequestDto.updateTask(task, request);
        TaskResponseDto response = new TaskMapper(mock(ReleaseRadarService.class)).toDto(task);

        assertNull(response.favoritePlaylistId());
        assertEquals(TasteSource.LIKED_SONGS, response.tasteSource());
    }

    @Test
    void rejectsIncompleteOrConflictingSourceSelections() {
        assertThrows(BadRequestException.class, () -> new TaskRequestDto(
                "Missing", Platform.SPOTIFY, 7, null, true, "discovery", null, TasteSource.PLAYLIST));
        assertThrows(BadRequestException.class, () -> new TaskRequestDto(
                "Conflicting", Platform.SPOTIFY, 7, null, true, "discovery", "favorites", TasteSource.LIKED_SONGS));
    }

    @Test
    void acceptsLegacyJsonWithoutAFavoritePlaylist() throws Exception {
        TaskRequestDto request = new ObjectMapper().readValue(
                "{\"name\":\"Legacy\",\"platform\":\"SPOTIFY\",\"active\":true,\"playlistId\":\"discovery\"}",
                TaskRequestDto.class);

        assertNull(TaskRequestDto.toTask(request).getFavoritePlaylistId());
        assertEquals("discovery", request.playlistId());
    }

    @Test
    void rejectsUsingTheFavoritePlaylistAsTheWriteTarget() {
        assertThrows(BadRequestException.class, () -> new TaskRequestDto(
                "Unsafe", Platform.SPOTIFY, 7, LocalDate.of(2026, 10, 30),
                true, "same-playlist", "same-playlist", TasteSource.PLAYLIST));
    }

    @Test
    void discoverySettingsDefaultToTwentyAndAppend() {
        TaskRequestDto request = new TaskRequestDto("Discovery", Platform.SPOTIFY, 7, null,
                true, "destination", null, TasteSource.LIKED_SONGS);
        Task task = TaskRequestDto.toTask(request);
        assertEquals(20, task.getDiscoveryLimit());
        assertFalse(task.isReplacePlaylist());
    }

    @Test
    void discoverySettingsSurviveJsonParsingAndMapping() throws Exception {
        TaskRequestDto request = new ObjectMapper().readValue(
                "{\"playlistId\":\"destination\",\"tasteSource\":\"LIKED_SONGS\",\"discoveryLimit\":8,\"replacePlaylist\":true}",
                TaskRequestDto.class);
        Task task = TaskRequestDto.toTask(request);
        assertEquals(8, task.getDiscoveryLimit());
        assertTrue(task.isReplacePlaylist());
    }

    @Test
    void rejectsInvalidLimitsAndMissingDestinations() {
        for (int limit : new int[]{0, 51}) {
            assertThrows(BadRequestException.class, () -> new TaskRequestDto("Discovery", Platform.SPOTIFY,
                    7, null, true, "destination", null, TasteSource.LIKED_SONGS, limit, false));
        }
        assertThrows(BadRequestException.class, () -> new TaskRequestDto("Discovery", Platform.SPOTIFY,
                7, null, true, null, "favorites", null));
    }

    static TaskRequestDto recent(List<String> sources, String favorite, Integer days) {
        return new TaskRequestDto("Fresh", Platform.SPOTIFY, 7, LocalDate.of(2026, 11, 6), true, "destination",
                favorite, TasteSource.RECENT_ADDITIONS, 20, false, sources, days);
    }

    @Test
    void recentAdditionsStoreSeveralSourcePlaylistsAndTheWindow() throws Exception {
        TaskRequestDto request = new ObjectMapper().readValue("""
                {"playlistId":"destination","tasteSource":"RECENT_ADDITIONS",
                 "sourcePlaylistIds":["mix-a"," mix-b ","mix-a",""],"recentDays":14}""", TaskRequestDto.class);
        Task task = TaskRequestDto.toTask(request);
        task.setId(42L);
        TaskResponseDto response = new TaskMapper(mock(ReleaseRadarService.class)).toDto(task);
        assertEquals(List.of("mix-a", "mix-b"), response.sourcePlaylistIds());
        assertEquals(14, response.recentDays());
        assertEquals(TasteSource.RECENT_ADDITIONS, response.tasteSource());

        TaskRequestDto.updateTask(task, new TaskRequestDto("Liked", Platform.SPOTIFY, 7, null, true,
                "destination", null, TasteSource.LIKED_SONGS));
        assertTrue(task.getSourcePlaylistIds().isEmpty());
        assertEquals(7, task.getRecentDays());
    }

    @Test
    void rejectsInvalidRecentAdditionSettings() {
        assertThrows(BadRequestException.class, () -> recent(List.of(), null, 7));
        assertThrows(BadRequestException.class, () -> recent(null, null, 7));
        assertThrows(BadRequestException.class, () -> recent(List.of("mix-a", "destination"), null, 7));
        assertThrows(BadRequestException.class, () -> recent(List.of("mix-a"), "favorites", 7));
        assertThrows(BadRequestException.class, () -> recent(List.of("mix-a"), null, 0));
        assertThrows(BadRequestException.class, () -> recent(List.of("mix-a"), null, 91));
        assertThrows(BadRequestException.class, () -> recent(
                java.util.stream.IntStream.range(0, 11).mapToObj(i -> "mix-" + i).toList(), null, 7));
        assertThrows(BadRequestException.class, () -> new TaskRequestDto("Liked", Platform.SPOTIFY, 7, null, true,
                "destination", null, TasteSource.LIKED_SONGS, 20, false, List.of("mix-a"), 7));
        assertEquals(7, recent(List.of("mix-a"), null, null).recentDays());
    }
}

package com.windschief.task;

import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.List;

import org.eclipse.microprofile.openapi.annotations.media.Schema;
import jakarta.ws.rs.BadRequestException;

public record TaskRequestDto(
        String name,
        Platform platform,
        int executionIntervalDays,
        LocalDate checkFrom,
        boolean active,
        String playlistId,
        @Schema(nullable = true) String favoritePlaylistId,
        @Schema(nullable = true) TasteSource tasteSource,
        Integer discoveryLimit,
        Boolean replacePlaylist,
        @Schema(nullable = true) List<String> sourcePlaylistIds,
        Integer recentDays) {
    public static final int MAX_SOURCE_PLAYLISTS = 10;

    public TaskRequestDto(String name, Platform platform, int executionIntervalDays, LocalDate checkFrom,
            boolean active, String playlistId, String favoritePlaylistId, TasteSource tasteSource) {
        this(name, platform, executionIntervalDays, checkFrom, active, playlistId, favoritePlaylistId, tasteSource, null, null);
    }
    public TaskRequestDto(String name, Platform platform, int executionIntervalDays, LocalDate checkFrom,
            boolean active, String playlistId, String favoritePlaylistId, TasteSource tasteSource,
            Integer discoveryLimit, Boolean replacePlaylist) {
        this(name, platform, executionIntervalDays, checkFrom, active, playlistId, favoritePlaylistId, tasteSource,
                discoveryLimit, replacePlaylist, null, null);
    }
    public TaskRequestDto {
        discoveryLimit = discoveryLimit == null ? 20 : discoveryLimit;
        replacePlaylist = Boolean.TRUE.equals(replacePlaylist);
        recentDays = recentDays == null ? 7 : recentDays;
        if (discoveryLimit < 1 || discoveryLimit > 50) {
            throw new BadRequestException("Choose between 1 and 50 discoveries per run");
        }
        if (recentDays < 1 || recentDays > 90) {
            throw new BadRequestException("Choose a look-back window between 1 and 90 days");
        }
        sourcePlaylistIds = sourcePlaylistIds == null ? List.of() : List.copyOf(new LinkedHashSet<>(
                sourcePlaylistIds.stream().filter(id -> id != null && !id.isBlank()).map(String::strip).toList()));
        if (tasteSource == TasteSource.RECENT_ADDITIONS) {
            if (sourcePlaylistIds.isEmpty()) throw new BadRequestException("Choose at least one source playlist");
            if (sourcePlaylistIds.size() > MAX_SOURCE_PLAYLISTS) {
                throw new BadRequestException("Choose at most " + MAX_SOURCE_PLAYLISTS + " source playlists");
            }
            if (favoritePlaylistId != null && !favoritePlaylistId.isBlank()) {
                throw new BadRequestException("Recently added songs use source playlists, not a favorite playlist");
            }
            if (sourcePlaylistIds.contains(playlistId)) {
                throw new BadRequestException("Source and discovery playlists must be different");
            }
        } else if (!sourcePlaylistIds.isEmpty()) {
            throw new BadRequestException("Source playlists require recently added songs as the taste source");
        }
        favoritePlaylistId = favoritePlaylistId == null || favoritePlaylistId.isBlank()
                ? null : favoritePlaylistId.strip();
        if (tasteSource == null && favoritePlaylistId != null) {
            tasteSource = TasteSource.PLAYLIST;
        }
        if (tasteSource != null) {
            checkFrom = null; // Personal discovery prefers releases from a rolling 30-day window instead.
        }
        if (tasteSource != null && (playlistId == null || playlistId.isBlank())) {
            throw new BadRequestException("Choose a discovery playlist");
        }
        if (tasteSource == TasteSource.PLAYLIST && favoritePlaylistId == null) {
            throw new BadRequestException("Choose a favorite playlist");
        }
        if (tasteSource == TasteSource.LIKED_SONGS && favoritePlaylistId != null) {
            throw new BadRequestException("Liked Songs cannot have a source playlist");
        }
        if (favoritePlaylistId != null && favoritePlaylistId.equals(playlistId)) {
            throw new BadRequestException("Favorite and discovery playlists must be different");
        }
    }

    public static Task toTask(TaskRequestDto taskRequestDto) {
        if (taskRequestDto == null) {
            throw new IllegalArgumentException("TaskRequestDto cannot be null");
        }
        Task task = new Task();
        task.setName(taskRequestDto.name());
        task.setPlatform(taskRequestDto.platform());
        task.setExecutionIntervalDays(taskRequestDto.executionIntervalDays());
        task.setCheckFrom(taskRequestDto.checkFrom());
        task.setActive(taskRequestDto.active());
        task.setPlaylistId(taskRequestDto.playlistId());
        task.setFavoritePlaylistId(taskRequestDto.favoritePlaylistId());
        task.setTasteSource(taskRequestDto.tasteSource());
        task.setDiscoveryLimit(taskRequestDto.discoveryLimit());
        task.setReplacePlaylist(taskRequestDto.replacePlaylist());
        task.setSourcePlaylistIds(taskRequestDto.sourcePlaylistIds());
        task.setRecentDays(taskRequestDto.recentDays());
        return task;
    }

    public static void updateTask(Task task, TaskRequestDto dto) {
        if (dto == null || task == null) {
            throw new IllegalArgumentException("Task and TaskRequestDto cannot be null");
        }
        task.setName(dto.name());
        task.setPlatform(dto.platform());
        task.setExecutionIntervalDays(dto.executionIntervalDays());
        task.setCheckFrom(dto.checkFrom());
        task.setActive(dto.active());
        task.setPlaylistId(dto.playlistId());
        task.setFavoritePlaylistId(dto.favoritePlaylistId());
        task.setTasteSource(dto.tasteSource());
        task.setDiscoveryLimit(dto.discoveryLimit());
        task.setReplacePlaylist(dto.replacePlaylist());
        task.setSourcePlaylistIds(dto.sourcePlaylistIds());
        task.setRecentDays(dto.recentDays());
    }
}

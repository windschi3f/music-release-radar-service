package com.windschief.releasedetection;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Year;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import com.windschief.auth.SpotifyTokenValidator;
import com.windschief.spotify.SpotifyApi;
import com.windschief.spotify.model.AlbumItem;
import com.windschief.spotify.model.AlbumsResponse;
import com.windschief.task.Platform;
import com.windschief.task.Task;
import com.windschief.task.added_item.AddedItemRepository;
import com.windschief.task.item.TaskItemType;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class ReleaseDetectionService {
    private final SpotifyApi spotifyApi;
    private final AddedItemRepository addedItemRepository;
    private final SpotifyTokenValidator spotifyTokenValidator;

    @Inject
    public ReleaseDetectionService(@RestClient SpotifyApi spotifyApi, AddedItemRepository addedItemRepository,
            SpotifyTokenValidator spotifyTokenValidator) {
        this.spotifyApi = spotifyApi;
        this.addedItemRepository = addedItemRepository;
        this.spotifyTokenValidator = spotifyTokenValidator;
    }

    public Set<String> detectNewAlbumIds(Task task) {
        if (task.getPlatform() != Platform.SPOTIFY) {
            throw new IllegalArgumentException("Unsupported platform: " + task.getPlatform());
        }
        if (task.getTaskItems().stream().anyMatch(item -> TaskItemType.PLAYLIST.equals(item.getItemType()))) {
            throw new IllegalArgumentException("Unsupported task item type: PLAYLIST");
        }

        final String accessToken = spotifyTokenValidator.getValidTokenForUser(task.getUserId());
        final Instant lastAddedAt = addedItemRepository.getLastAddedAt(task.getId());

        return task.getTaskItems().stream()
                .map(item -> item.getExternalReferenceId())
                .map(artistId -> getAllAlbums(accessToken, artistId))
                .flatMap(albums -> albums.stream())
                .filter(album -> isAlbumAfterDate(album, lastAddedAt))
                .filter(album -> !addedItemRepository.existsByExternalIdAndTaskId(album.id(), task.getId()))
                .map(album -> album.id())
                .collect(Collectors.toSet());
    }

    private List<AlbumItem> getAllAlbums(String accessToken, String artistId) {
        final List<AlbumItem> allAlbums = new ArrayList<>();

        AlbumsResponse response = spotifyApi.getArtistAlbums(accessToken, artistId, "album,single", 50, 0);
        while (true) {
            allAlbums.addAll(response.items());
            if (response.next() == null) {
                break;
            }
            response = spotifyApi.getNextPage(accessToken, AlbumsResponse.class);
        }

        return allAlbums;
    }

    private boolean isAlbumAfterDate(AlbumItem album, Instant lastAddedAt) {
        if (lastAddedAt == null) {
            return true;
        }

        final LocalDateTime releaseDateTime = switch (album.release_date_precision()) {
            case "day" -> LocalDate.parse(album.release_date()).atStartOfDay();
            case "month" -> YearMonth.parse(album.release_date()).atDay(1).atStartOfDay();
            case "year" -> Year.parse(album.release_date()).atDay(1).atStartOfDay();
            default -> throw new IllegalArgumentException("Unknown date precision: " + album.release_date_precision());
        };

        return releaseDateTime.isAfter(LocalDateTime.ofInstant(lastAddedAt, ZoneOffset.UTC));
    }
}

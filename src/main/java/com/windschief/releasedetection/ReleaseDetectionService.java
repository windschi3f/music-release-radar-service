package com.windschief.releasedetection;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Year;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import com.windschief.auth.SpotifyTokenService;
import com.windschief.spotify.SpotifyApi;
import com.windschief.spotify.model.AlbumItem;
import com.windschief.spotify.model.AlbumsResponse;
import com.windschief.task.Platform;
import com.windschief.task.Task;
import com.windschief.task.added_item.AddedItemRepository;
import com.windschief.task.item.TaskItem;
import com.windschief.task.item.TaskItemType;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;

@ApplicationScoped
public class ReleaseDetectionService {
    private final SpotifyApi spotifyApi;
    private final AddedItemRepository addedItemRepository;
    private final SpotifyTokenService spotifyTokenService;

    @Inject
    public ReleaseDetectionService(@RestClient SpotifyApi spotifyApi, AddedItemRepository addedItemRepository,
            SpotifyTokenService spotifyTokenService) {
        this.spotifyApi = spotifyApi;
        this.addedItemRepository = addedItemRepository;
        this.spotifyTokenService = spotifyTokenService;
    }

    public Set<String> detectNewAlbumIds(Task task) throws WebApplicationException {
        if (task.getPlatform() != Platform.SPOTIFY) {
            throw new IllegalArgumentException("Unsupported platform: " + task.getPlatform());
        }
        if (task.getTaskItems().stream().anyMatch(item -> TaskItemType.PLAYLIST.equals(item.getItemType()))) {
            throw new IllegalArgumentException("Unsupported task item type: PLAYLIST");
        }

        final String accessToken = spotifyTokenService.getValidToken(task.getUserId());
        final Instant lastAddedAt = addedItemRepository.getLastAddedAt(task.getId());
        final List<String> artistIds = task.getTaskItems().stream()
                .map(TaskItem::getExternalReferenceId)
                .toList();

        final Set<String> albumIds = new HashSet<>();
        for (String artistId : artistIds) {
            final List<AlbumItem> albums = getAllAlbums(accessToken, artistId);
            albums.stream()
                    .filter(album -> isAlbumAfterDate(album, lastAddedAt))
                    .filter(album -> !addedItemRepository.existsByExternalIdAndTaskId(album.id(), task.getId()))
                    .map(AlbumItem::id)
                    .forEach(albumIds::add);
        }

        return albumIds;
    }

    private List<AlbumItem> getAllAlbums(String accessToken, String artistId) throws WebApplicationException {
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

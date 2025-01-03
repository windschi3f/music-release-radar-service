package com.windschief.releasedetection;

import java.io.IOException;
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
import com.windschief.client.HttpClientService;
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
import jakarta.transaction.Transactional;
import jakarta.ws.rs.WebApplicationException;

@ApplicationScoped
public class ReleaseDetectionService {
    private final SpotifyApi spotifyApi;
    private final AddedItemRepository addedItemRepository;
    private final SpotifyTokenService spotifyTokenService;
    private final HttpClientService httpClientService;

    @Inject
    public ReleaseDetectionService(@RestClient SpotifyApi spotifyApi, AddedItemRepository addedItemRepository,
            SpotifyTokenService spotifyTokenService, HttpClientService httpClientService) {
        this.spotifyApi = spotifyApi;
        this.addedItemRepository = addedItemRepository;
        this.spotifyTokenService = spotifyTokenService;
        this.httpClientService = httpClientService;
    }

    @Transactional
    public Set<String> detectNewAlbumIds(Task task) throws WebApplicationException, IOException, InterruptedException {
        if (task.getPlatform() != Platform.SPOTIFY) {
            throw new IllegalArgumentException("Unsupported platform: " + task.getPlatform());
        }
        if (task.getTaskItems().stream().anyMatch(item -> TaskItemType.PLAYLIST.equals(item.getItemType()))) {
            throw new IllegalArgumentException("Unsupported task item type: PLAYLIST");
        }

        final String bearerToken = spotifyTokenService.getValidBearerAccessToken(task.getUserId());
        final List<String> artistIds = task.getTaskItems().stream()
                .map(TaskItem::getExternalReferenceId)
                .toList();

        final Set<String> albumIds = new HashSet<>();
        for (String artistId : artistIds) {
            final List<AlbumItem> albums = getAllAlbums(bearerToken, artistId);
            albums.stream()
                    .filter(album -> isAlbumAfterDate(album, task.getCheckFrom()))
                    .filter(album -> !addedItemRepository.existsByExternalIdAndTaskId(album.id(), task.getId()))
                    .map(AlbumItem::id)
                    .forEach(albumIds::add);
        }

        return albumIds;
    }

    private List<AlbumItem> getAllAlbums(String bearerToken, String artistId)
            throws WebApplicationException, IOException, InterruptedException {
        final List<AlbumItem> allAlbums = new ArrayList<>();

        AlbumsResponse response = spotifyApi.getArtistAlbums(bearerToken, artistId, "album,single", 50, 0);
        while (true) {
            allAlbums.addAll(response.items());
            if (response.next() == null) {
                break;
            }

            response = httpClientService.get(response.next(), bearerToken, AlbumsResponse.class);
        }

        return allAlbums;
    }

    private boolean isAlbumAfterDate(AlbumItem album, Instant checkFrom) {
        final LocalDateTime releaseDateTime = switch (album.release_date_precision()) {
            case "day" -> LocalDate.parse(album.release_date()).atStartOfDay();
            case "month" -> YearMonth.parse(album.release_date()).atDay(1).atStartOfDay();
            case "year" -> Year.parse(album.release_date()).atDay(1).atStartOfDay();
            default -> throw new IllegalArgumentException("Unknown date precision: " + album.release_date_precision());
        };

        return releaseDateTime.isAfter(LocalDateTime.ofInstant(checkFrom, ZoneOffset.UTC))
                || releaseDateTime.isEqual(LocalDateTime.ofInstant(checkFrom, ZoneOffset.UTC));
    }
}

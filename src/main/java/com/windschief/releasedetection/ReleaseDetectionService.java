package com.windschief.releasedetection;

import java.io.IOException;
import java.time.LocalDate;
import java.time.Year;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import com.windschief.auth.SpotifyTokenException;
import com.windschief.auth.SpotifyTokenService;
import com.windschief.client.HttpClientService;
import com.windschief.spotify.SpotifyApi;
import com.windschief.spotify.model.AlbumItem;
import com.windschief.spotify.model.AlbumsResponse;
import com.windschief.task.Platform;
import com.windschief.task.Task;
import com.windschief.task.TaskRepository;
import com.windschief.task.added_item.AddedItemRepository;
import com.windschief.task.added_item.AddedItemType;
import com.windschief.task.item.TaskItem;
import com.windschief.task.item.TaskItemType;

import io.micrometer.core.annotation.Counted;
import io.micrometer.core.annotation.Timed;
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
    private final TaskRepository taskRepository;

    @Inject
    public ReleaseDetectionService(@RestClient SpotifyApi spotifyApi, AddedItemRepository addedItemRepository,
            SpotifyTokenService spotifyTokenService, HttpClientService httpClientService,
            TaskRepository taskRepository) {
        this.spotifyApi = spotifyApi;
        this.addedItemRepository = addedItemRepository;
        this.spotifyTokenService = spotifyTokenService;
        this.httpClientService = httpClientService;
        this.taskRepository = taskRepository;
    }

    @Counted(value = "release.detection.operations")
    @Timed(value = "release.detection.operation.duration")
    public List<AlbumItem> detectNewAlbumReleases(long taskId)
            throws WebApplicationException, IOException, InterruptedException, SpotifyTokenException {
        final Task task = loadAndValidateTask(taskId);

        final String token = spotifyTokenService.getValidBearerAccessToken(task.getUserId());

        return findNewReleasesFromArtists(task, token);
    }

    @Transactional
    protected Task loadAndValidateTask(Long taskId) {
        Task task = taskRepository.findById(taskId);

        task.getTaskItems().size(); // trigger lazy loading

        if (task.getPlatform() != Platform.SPOTIFY) {
            throw new IllegalArgumentException("Unsupported platform: " + task.getPlatform());
        }
        if (task.getTaskItems().stream().anyMatch(item -> TaskItemType.PLAYLIST.equals(item.getItemType()))) {
            throw new IllegalArgumentException("Unsupported task item type: PLAYLIST");
        }

        return task;
    }

    private List<AlbumItem> findNewReleasesFromArtists(Task task, String token)
            throws WebApplicationException, IOException, InterruptedException {
        final List<String> artistIds = task.getTaskItems().stream()
                .map(TaskItem::getExternalReferenceId)
                .toList();

        final List<AlbumItem> albums = new ArrayList<>();
        for (String artistId : artistIds) {
            fetchAllArtistAlbums(token, artistId).stream()
                    .filter(album -> isAlbumReleasedOnOrAfter(album, task.getCheckFrom()))
                    .filter(album -> !isAlbumAlreadyAdded(album.id(), task.getId()))
                    .forEach(albums::add);
        }

        return albums;
    }

    @Timed(value = "spotify.albums.fetch.duration", description = "Time taken to fetch all albums for an artist")
    protected List<AlbumItem> fetchAllArtistAlbums(String token, String artistId)
            throws WebApplicationException, IOException, InterruptedException {
        final List<AlbumItem> albums = new ArrayList<>();

        AlbumsResponse response = spotifyApi.getArtistAlbums(token, artistId, "album,single", 50, 0);
        while (true) {
            albums.addAll(response.items());
            if (response.next() == null) {
                break;
            }

            response = httpClientService.get(response.next(), token, AlbumsResponse.class);
        }

        return albums;
    }

    private boolean isAlbumReleasedOnOrAfter(AlbumItem album, LocalDate checkFrom) {
        final LocalDate releaseDate = switch (album.release_date_precision()) {
            case "day" -> LocalDate.parse(album.release_date());
            case "month" -> YearMonth.parse(album.release_date()).atEndOfMonth();
            case "year" -> Year.parse(album.release_date()).atDay(Year.parse(album.release_date()).length());
            default -> throw new IllegalArgumentException("Unknown date precision: " + album.release_date_precision());
        };

        return !releaseDate.isBefore(checkFrom);
    }

    @Transactional
    protected boolean isAlbumAlreadyAdded(String albumId, Long taskId) {
        return addedItemRepository.existsByTaskIdAndExternalIdAndItemType(taskId, albumId, AddedItemType.ALBUM);
    }
}

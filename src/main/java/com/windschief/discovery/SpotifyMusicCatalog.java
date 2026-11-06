package com.windschief.discovery;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class SpotifyMusicCatalog implements MusicCatalog {
    private final URI base;
    private final ObjectMapper mapper;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

    @Inject
    public SpotifyMusicCatalog(
            @ConfigProperty(name = "discovery.spotify-base-url", defaultValue = "https://api.spotify.com/v1/") String base,
            ObjectMapper mapper) {
        this.base = URI.create(base.endsWith("/") ? base : base + "/");
        this.mapper = mapper;
    }

    @Override
    public List<DiscoveryTrack> savedTracks(String token) {
        return pages("me/tracks?limit=50", token, item -> track(item.path("track"), null));
    }

    @Override
    public List<DiscoveryTrack> playlistTracks(String token, String playlistId) {
        return playlistAdditions(token, playlistId).stream().map(PlaylistAddition::track).toList();
    }

    @Override
    public List<PlaylistAddition> playlistAdditions(String token, String playlistId) {
        return pages("playlists/" + id(playlistId) + "/items?limit=50", token, item -> {
            DiscoveryTrack track = track(item.has("item") ? item.path("item") : item.path("track"), null);
            return track == null ? null : new PlaylistAddition(track, instant(item.path("added_at").asText()));
        });
    }

    @Override
    public List<DiscoveryTrack> artistTracks(String token, String artistId) {
        JsonNode albums = request("artists/" + id(artistId) + "/albums?include_groups=album,single&limit=20",
                token, "GET", null);
        if (!albums.path("items").isArray()) throw invalidResponse();
        List<DiscoveryTrack> tracks = new ArrayList<>();
        var seen = new HashSet<String>();
        for (JsonNode album : albums.path("items")) {
            String albumId = album.path("id").asText();
            if (!seen.add(albumId)) continue;
            tracks.addAll(pages("albums/" + id(albumId) + "/tracks?limit=50", token, item -> track(item, album)));
            if (seen.size() >= 3) break;
        }
        return tracks;
    }

    @Override
    public Optional<DiscoveryTrack> resolve(String token, String artist, String title) {
        String query = "track:\"" + title.replace("\"", "") + "\" artist:\"" + artist.replace("\"", "") + "\"";
        JsonNode result = request("search?type=track&limit=5&q=" + encode(query), token, "GET", null);
        for (JsonNode item : result.path("tracks").path("items")) {
            DiscoveryTrack track = track(item, null);
            if (track != null && DiscoveryTrack.normalizeTitle(track.name()).equals(DiscoveryTrack.normalizeTitle(title))
                    && track.artists().stream().anyMatch(a ->
                        DiscoveryTrack.normalize(a.name()).equals(DiscoveryTrack.normalize(artist)))) {
                return Optional.of(track);
            }
        }
        return Optional.empty();
    }

    @Override
    public void writePlaylist(String token, String playlistId, List<DiscoveryTrack> tracks, boolean replace) {
        if (tracks.isEmpty()) return; // Never empty a playlist because no candidates were found.
        if (tracks.size() > 50) throw new DiscoveryException("Choose at most 50 discoveries per run.");
        try {
            String body = mapper.writeValueAsString(java.util.Map.of("uris", tracks.stream().map(DiscoveryTrack::uri).toList()));
            request("playlists/" + id(playlistId) + "/items", token, replace ? "PUT" : "POST", body);
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new DiscoveryException("Could not prepare the discovery playlist.");
        }
    }

    private <T> List<T> pages(String first, String token, Function<JsonNode, T> extract) {
        List<T> result = new ArrayList<>();
        var visited = new HashSet<String>();
        String next = first;
        while (next != null) {
            if (!visited.add(next) || visited.size() > 400) {
                throw new DiscoveryException("This music collection is too large or could not be read completely.");
            }
            JsonNode page = request(next, token, "GET", null);
            if (!page.path("items").isArray()) throw invalidResponse();
            for (JsonNode item : page.path("items")) {
                T value = extract.apply(item);
                if (value != null) result.add(value);
            }
            next = page.path("next").isTextual() ? page.path("next").asText() : null;
        }
        return result;
    }

    private JsonNode request(String path, String token, String method, String body) {
        URI uri = base.resolve(path);
        if (!base.getScheme().equals(uri.getScheme()) || !base.getHost().equals(uri.getHost())
                || base.getPort() != uri.getPort() || !uri.getPath().startsWith(base.getPath())) {
            throw invalidResponse(); // Never forward a bearer token to a pagination URL on another host.
        }
        HttpRequest.Builder builder = HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(20))
                .header("Authorization", token).header("Accept", "application/json");
        if (body == null) builder.GET();
        else builder.header("Content-Type", "application/json").method(method, HttpRequest.BodyPublishers.ofString(body));
        try {
            HttpResponse<String> response = http.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            // Existing applications may still expose only the older playlist endpoint.
            if ((response.statusCode() == 404 || response.statusCode() == 405) && path.contains("/items")) {
                return request(path.replace("/items", "/tracks"), token, method, body);
            }
            if (response.statusCode() == 401 || response.statusCode() == 403) {
                throw new DiscoveryException("Reconnect Spotify to grant access to your songs and discovery playlist.");
            }
            if (response.statusCode() == 429) throw new DiscoveryException("Spotify's request limit was reached. Please try again later.");
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new DiscoveryException("Spotify could not complete this run. Please try again later.");
            }
            return response.body().isBlank() ? mapper.createObjectNode() : mapper.readTree(response.body());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new DiscoveryException("The discovery run was interrupted.");
        } catch (IOException e) {
            throw new DiscoveryException("Spotify could not be reached. Your playlist may need checking before retrying.");
        }
    }

    private DiscoveryTrack track(JsonNode item, JsonNode parentAlbum) {
        if (item == null || item.isNull() || item.path("is_local").asBoolean()
                || !item.path("type").asText("track").equals("track")
                || (item.has("is_playable") && !item.path("is_playable").asBoolean())) return null;
        String trackId = item.path("id").asText();
        String name = item.path("name").asText();
        if (trackId.isBlank() || name.isBlank()) return null;
        List<DiscoveryTrack.Artist> artists = new ArrayList<>();
        for (JsonNode a : item.path("artists")) {
            if (!a.path("id").asText().isBlank() && !a.path("name").asText().isBlank()) {
                artists.add(new DiscoveryTrack.Artist(a.path("id").asText(), a.path("name").asText()));
            }
        }
        if (artists.isEmpty()) return null;
        JsonNode album = parentAlbum == null ? item.path("album") : parentAlbum;
        String isrc = item.path("external_ids").path("isrc").asText("");
        return new DiscoveryTrack(trackId, name, List.copyOf(artists), album.path("id").asText(),
                date(album.path("release_date").asText()), isrc.isBlank() ? null : isrc.toUpperCase(java.util.Locale.ROOT));
    }

    private static Instant instant(String value) {
        try {
            return value == null || value.isBlank() ? null : Instant.parse(value);
        } catch (java.time.DateTimeException e) { return null; }
    }

    private LocalDate date(String value) {
        try {
            if (value.length() == 4) return java.time.Year.parse(value).atDay(1);
            if (value.length() == 7) return java.time.YearMonth.parse(value).atDay(1);
            return LocalDate.parse(value);
        } catch (java.time.DateTimeException e) { return null; }
    }

    private String id(String value) {
        if (value == null || !value.matches("[A-Za-z0-9_-]+")) throw new DiscoveryException("Choose a valid Spotify playlist or artist.");
        return value;
    }
    private static String encode(String value) { return URLEncoder.encode(value, StandardCharsets.UTF_8); }
    private DiscoveryException invalidResponse() { return new DiscoveryException("Spotify returned an incomplete music collection. Please try again."); }
}

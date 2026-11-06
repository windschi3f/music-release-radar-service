package com.windschief.discovery;

import static org.junit.jupiter.api.Assertions.*;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;

class DiscoveryHttpClientsTest {
    record Reply(int status, String body) {}
    HttpServer server;
    String base;
    List<String> requests = new ArrayList<>();
    Function<String, Reply> responder;
    final ObjectMapper mapper = new ObjectMapper();
    static final String TRACK = "{\"id\":\"one\",\"name\":\"A Song\",\"type\":\"track\",\"artists\":[{\"id\":\"artist\",\"name\":\"Artist\"}],\"album\":{\"id\":\"album\",\"release_date\":\"2020-01-01\"},\"external_ids\":{\"isrc\":\"ISRC1\"}}";

    @BeforeEach
    void start() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        base = "http://127.0.0.1:" + server.getAddress().getPort() + "/";
        server.createContext("/", exchange -> {
            String key = exchange.getRequestMethod() + " " + exchange.getRequestURI();
            requests.add(key);
            Reply reply = responder.apply(key);
            byte[] body = reply.body().getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(reply.status(), body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();
    }
    @AfterEach void stop() { server.stop(0); }

    SpotifyMusicCatalog catalog() { return new SpotifyMusicCatalog(base + "v1/", mapper); }

    @Test
    void readsEverySavedSongPageAndSkipsUnavailableItems() {
        responder = path -> path.contains("offset=1")
                ? new Reply(200, "{\"items\":[{\"track\":null},{\"track\":" + TRACK.replace("\"one\"", "\"two\"") + "}],\"next\":null}")
                : new Reply(200, "{\"items\":[{\"track\":" + TRACK + "}],\"next\":\"" + base + "v1/me/tracks?offset=1\"}");
        var tracks = catalog().savedTracks("Bearer fixture");
        assertEquals(List.of("one", "two"), tracks.stream().map(DiscoveryTrack::id).toList());
        assertEquals(2, requests.size());
    }

    @Test
    void doesNotReturnAPartialLibraryOrFollowUntrustedPagination() {
        responder = path -> path.contains("offset=1") ? new Reply(500, "{}")
                : new Reply(200, "{\"items\":[{\"track\":" + TRACK + "}],\"next\":\"" + base + "v1/me/tracks?offset=1\"}");
        assertThrows(DiscoveryException.class, () -> catalog().savedTracks("Bearer fixture"));
        responder = path -> new Reply(200, "{\"items\":[],\"next\":\"https://example.invalid/steal\"}");
        int before = requests.size();
        assertThrows(DiscoveryException.class, () -> catalog().savedTracks("Bearer fixture"));
        assertEquals(before + 1, requests.size());
    }

    @Test
    void supportsBothPlaylistFormatsAndFallsBackOnlyForMissingRoutes() {
        responder = path -> path.contains("/items") ? new Reply(404, "{}")
                : new Reply(200, "{\"items\":[{\"track\":" + TRACK + "}],\"next\":null}");
        assertEquals(1, catalog().playlistTracks("Bearer fixture", "playlist").size());
        assertEquals(2, requests.size());
        requests.clear();
        responder = path -> new Reply(200, "{\"items\":[{\"item\":" + TRACK + "}],\"next\":null}");
        assertEquals(1, catalog().playlistTracks("Bearer fixture", "playlist").size());
        requests.clear();
        responder = path -> new Reply(403, "{}");
        assertThrows(DiscoveryException.class, () -> catalog().playlistTracks("Bearer fixture", "playlist"));
        assertEquals(1, requests.size());
    }

    @Test
    void replacesOnlyWhenRequestedAndNeverClearsAnEmptySelection() {
        responder = path -> new Reply(200, "{}");
        DiscoveryTrack track = PersonalDiscoveryServiceTest.track("one", "A Song", "Artist", "album", "REC");
        catalog().writePlaylist("Bearer fixture", "playlist", List.of(track), true);
        catalog().writePlaylist("Bearer fixture", "playlist", List.of(track), false);
        catalog().writePlaylist("Bearer fixture", "playlist", List.of(), true);
        assertTrue(requests.get(0).startsWith("PUT "));
        assertTrue(requests.get(1).startsWith("POST "));
        assertEquals(2, requests.size());
    }

    @Test
    void resolvesOnlyMatchingTitlesAndArtists() {
        responder = path -> new Reply(200, "{\"tracks\":{\"items\":["
                + TRACK.replace("\"Artist\"", "\"Wrong Artist\"") + "," + TRACK + "]}}");
        assertEquals("one", catalog().resolve("Bearer fixture", "Artist", "A Song").orElseThrow().id());
        assertTrue(catalog().resolve("Bearer fixture", "Other Artist", "A Song").isEmpty());
        responder = path -> new Reply(200, "{\"tracks\":{\"items\":[" + TRACK.replace("A Song", "A Song - 2026 Remaster") + "]}}");
        assertTrue(catalog().resolve("Bearer fixture", "Artist", "A Song").isPresent());
    }

    @Test
    void lastFmIsOptionalAndCachesPublicTrackMatches() {
        var disabled = new LastFmSimilarSongs(Optional.empty(), Optional.empty(), base, mapper);
        assertFalse(disabled.available());
        assertTrue(disabled.find(PersonalDiscoveryServiceTest.track("one", "Seed", "Artist", "album", null)).isEmpty());
        assertTrue(requests.isEmpty());
        responder = path -> new Reply(200, "{\"similartracks\":{\"track\":[{\"name\":\"Gem\",\"match\":\"0.9\",\"artist\":{\"name\":\"New Artist\"}}]}}");
        var client = new LastFmSimilarSongs(Optional.of("fixture-key"), Optional.empty(), base, mapper);
        var seed = PersonalDiscoveryServiceTest.track("one", "Seed", "Artist", "album", null);
        assertEquals("Gem", client.find(seed).getFirst().title());
        assertEquals(1, client.find(seed).size());
        assertEquals(1, requests.size());
    }

    @Test
    void lastFmErrorsNeverExposeTheApiKeyAndKeyFilesAreSupported(@TempDir Path dir) throws Exception {
        responder = path -> new Reply(200, "{\"error\":10,\"message\":\"bad key\"}");
        Path keyFile = dir.resolve("api-key");
        Files.writeString(keyFile, "private-fixture-key\n");
        var client = new LastFmSimilarSongs(Optional.empty(), Optional.of(keyFile.toString()), base, mapper);
        assertTrue(client.available());
        var failure = assertThrows(DiscoveryException.class,
                () -> client.find(PersonalDiscoveryServiceTest.track("one", "Seed", "Artist", "album", null)));
        assertFalse(failure.getMessage().contains("private-fixture-key"));
        assertNull(failure.getCause());
    }

    @Test
    void readsWhenEachPlaylistSongWasAdded() {
        responder = path -> new Reply(200, "{\"items\":[{\"added_at\":\"2026-11-01T10:15:00Z\",\"item\":" + TRACK
                + "},{\"added_at\":null,\"track\":" + TRACK.replace("\"one\"", "\"two\"") + "},{\"added_at\":\"x\",\"track\":null}],\"next\":null}");
        var additions = catalog().playlistAdditions("Bearer fixture", "playlist");
        assertEquals(List.of("one", "two"), additions.stream().map(a -> a.track().id()).toList());
        assertEquals(java.time.Instant.parse("2026-11-01T10:15:00Z"), additions.get(0).addedAt());
        assertNull(additions.get(1).addedAt());
    }
}

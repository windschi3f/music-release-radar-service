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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class LastFmSimilarSongs implements SimilarSongs {
    private record Cached(Instant expires, List<Match> matches) {}
    private final String key;
    private final String base;
    private final ObjectMapper mapper;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    private final ConcurrentHashMap<String, Cached> cache = new ConcurrentHashMap<>();

    @Inject
    public LastFmSimilarSongs(
            @ConfigProperty(name = "lastfm.api-key") java.util.Optional<String> key,
            @ConfigProperty(name = "lastfm.api-key-file") java.util.Optional<String> keyFile,
            @ConfigProperty(name = "lastfm.base-url", defaultValue = "https://ws.audioscrobbler.com/2.0/") String base,
            ObjectMapper mapper) {
        this.key = key.filter(value -> !value.isBlank()).orElseGet(() -> {
            if (keyFile.isEmpty()) return "";
            try {
                return java.nio.file.Files.readString(java.nio.file.Path.of(keyFile.get())).trim();
            } catch (IOException e) {
                throw new IllegalStateException("The configured Last.fm key file could not be read.");
            }
        });
        this.base = base;
        this.mapper = mapper;
    }

    @Override public boolean available() { return key != null && !key.isBlank(); }

    @Override
    public List<Match> find(DiscoveryTrack seed) {
        if (!available()) return List.of();
        String cacheKey = seed.recordingKey();
        Cached cached = cache.get(cacheKey);
        if (cached != null && cached.expires().isAfter(Instant.now())) return cached.matches();
        String query = "?method=track.getsimilar&format=json&autocorrect=1&limit=10"
                + "&artist=" + enc(seed.primaryArtist()) + "&track=" + enc(seed.name()) + "&api_key=" + enc(key);
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(base + query))
                    .timeout(Duration.ofSeconds(15)).GET().build();
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) throw unavailable();
            JsonNode root = mapper.readTree(response.body());
            if (root == null || !root.isObject() || root.has("error")) throw unavailable();
            JsonNode tracks = root.path("similartracks").path("track");
            if (!tracks.isArray()) throw unavailable();
            List<Match> matches = new ArrayList<>();
            for (JsonNode track : tracks) {
                String title = track.path("name").asText();
                String artist = track.path("artist").path("name").asText();
                double score = track.path("match").asDouble(0);
                if (!title.isBlank() && !artist.isBlank() && Double.isFinite(score) && score > 0) {
                    matches.add(new Match(artist, title, Math.min(1, score)));
                }
            }
            if (cache.size() >= 500) cache.clear();
            List<Match> result = List.copyOf(matches);
            cache.put(cacheKey, new Cached(Instant.now().plus(Duration.ofHours(24)), result));
            return result;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw unavailable();
        } catch (IOException | IllegalArgumentException e) {
            // Do not expose request URLs: they contain the API key.
            throw unavailable();
        }
    }
    private static String enc(String value) { return URLEncoder.encode(value, StandardCharsets.UTF_8); }
    private DiscoveryException unavailable() { return new DiscoveryException("Similar-song search is temporarily unavailable."); }
}

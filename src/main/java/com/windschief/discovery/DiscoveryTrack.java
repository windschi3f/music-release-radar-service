package com.windschief.discovery;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.text.Normalizer;
import java.time.LocalDate;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;

public record DiscoveryTrack(String id, String name, List<Artist> artists, String albumId,
        LocalDate releaseDate, String isrc) {
    public record Artist(String id, String name) {}
    public String uri() { return "spotify:track:" + id; }
    public String primaryArtist() { return artists.getFirst().name(); }
    public String artistKey() { return artists.getFirst().id(); }
    public String recordingKey() {
        String artistNames = artists.stream().map(a -> normalize(a.name())).sorted()
                .reduce((a, c) -> a + "|" + c).orElse("");
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest((normalizeTitle(name) + "|" + artistNames).getBytes(StandardCharsets.UTF_8)));
        } catch (java.security.NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
    }
    public static String normalizeTitle(String value) {
        return normalize(value.replaceAll("(?i)\\s*\\([^)]*remaster[^)]*\\)\\s*$", "")
                .replaceAll("(?i)\\s*-\\s*(?:\\d{4}\\s+)?remaster(?:ed)?(?:\\s+\\d{4})?\\s*$", ""));
    }
    public static String normalize(String value) {
        return Normalizer.normalize(value == null ? "" : value, Normalizer.Form.NFKD)
                .replaceAll("\\p{M}+", "").toLowerCase(Locale.ROOT)
                .replaceAll("[^\\p{L}\\p{N}]+", " ").trim();
    }
}

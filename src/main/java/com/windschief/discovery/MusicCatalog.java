package com.windschief.discovery;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface MusicCatalog {
    /** A playlist entry; {@code addedAt} is null for very old playlist entries without a timestamp. */
    record PlaylistAddition(DiscoveryTrack track, Instant addedAt) {}
    List<DiscoveryTrack> savedTracks(String token);
    List<DiscoveryTrack> playlistTracks(String token, String playlistId);
    List<PlaylistAddition> playlistAdditions(String token, String playlistId);
    List<DiscoveryTrack> artistTracks(String token, String artistId);
    Optional<DiscoveryTrack> resolve(String token, String artist, String title);
    void writePlaylist(String token, String playlistId, List<DiscoveryTrack> tracks, boolean replace);
}

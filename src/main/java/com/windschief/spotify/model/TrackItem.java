package com.windschief.spotify.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public record TrackItem(
        AlbumItem album,
        List<ArtistItem> artists,
        @JsonProperty("available_markets")
        List<String> availableMarkets,
        @JsonProperty("disc_number")
        int discNumber,
        @JsonProperty("duration_ms")
        int durationMs,
        boolean explicit,
        @JsonProperty("external_ids")
        ExternalIds externalIds,
        @JsonProperty("external_urls")
        ExternalUrls externalUrls,
        String href,
        String id,
        boolean is_playable,
        @JsonProperty("linked_from")
        LinkedFrom linkedFrom,
        Restrictions restrictions,
        String name,
        int popularity,
        @JsonProperty("preview_url")
        String previewUrl,
        @JsonProperty("track_number")
        int trackNumber,
        String type,
        String uri,
        boolean is_local
) {}

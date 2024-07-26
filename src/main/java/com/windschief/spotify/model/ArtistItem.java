package com.windschief.spotify.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ArtistItem(
        @JsonProperty("external_urls")
        ExternalUrls externalUrls,
        Followers followers,
        List<String> genres,
        String href,
        String id,
        List<Image> images,
        String name,
        int popularity,
        String type,
        String uri
) {
}

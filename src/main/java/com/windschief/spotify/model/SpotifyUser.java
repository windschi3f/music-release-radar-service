package com.windschief.spotify.model;

import java.util.List;
import com.fasterxml.jackson.annotation.JsonProperty;

public record SpotifyUser(
        String id,
        @JsonProperty("display_name") String displayName,
        String email,
        String country,
        @JsonProperty("explicit_content") ExplicitContent explicitContent,
        @JsonProperty("external_urls") ExternalUrls externalUrls,
        Followers followers,
        String href,
        List<Image> images,
        String product,
        String type,
        String uri) {
}


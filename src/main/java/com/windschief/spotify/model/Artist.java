package com.windschief.spotify.model;

import java.util.List;

public record Artist(
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

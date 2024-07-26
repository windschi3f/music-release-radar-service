package com.windschief.spotify.model;

public record LinkedFrom(
        ExternalUrls external_urls,
        String href,
        String id,
        String type,
        String uri
) {}

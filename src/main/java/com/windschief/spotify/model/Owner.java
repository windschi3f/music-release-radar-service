package com.windschief.spotify.model;

public record Owner(
        ExternalUrls external_urls,
        Followers followers,
        String href,
        String id,
        String type,
        String uri,
        String display_name
) {}

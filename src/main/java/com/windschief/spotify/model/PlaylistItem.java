package com.windschief.spotify.model;

import java.util.List;

public record PlaylistItem(
        boolean collaborative,
        String description,
        ExternalUrls external_urls,
        String href,
        String id,
        List<Image> images,
        String name,
        Owner owner,
        String primary_color,
        boolean public_,
        String snapshot_id,
        Tracks tracks,
        String type,
        String uri
) {}

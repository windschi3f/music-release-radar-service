package com.windschief.spotify.model;

import java.util.List;

public record AlbumsResponse(
        String href,
        int limit,
        String next,
        int offset,
        String previous,
        int total,
        List<AlbumItem> items
) {}

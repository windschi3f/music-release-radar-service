package com.windschief.spotify.model;

import java.util.List;

public record PlaylistsResponse(
        String href,
        List<PlaylistItem> items,
        int limit,
        String next,
        int offset,
        String previous,
        int total
) {}

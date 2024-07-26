package com.windschief.spotify.model;

import java.util.List;

public record TracksResponse(
        String href,
        List<TrackItem> items,
        int limit,
        String next,
        int offset,
        String previous,
        int total
) {}

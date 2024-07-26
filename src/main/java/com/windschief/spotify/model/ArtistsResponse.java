package com.windschief.spotify.model;

import java.util.List;

public record ArtistsResponse(
        String href,
        int limit,
        String next,
        Cursors cursors,
        int total,
        List<ArtistItem> items) {
}

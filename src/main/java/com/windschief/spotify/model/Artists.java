package com.windschief.spotify.model;

import java.util.List;

public record Artists(
        String href,
        int limit,
        String next,
        Cursors cursors,
        int total,
        List<Artist> items) {
}

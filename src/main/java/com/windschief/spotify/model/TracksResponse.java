package com.windschief.spotify.model;

import java.util.List;

public record TracksResponse(
                String href,
                int limit,
                String next,
                int offset,
                String previous,
                int total,
                List<TrackItem> items) {
}

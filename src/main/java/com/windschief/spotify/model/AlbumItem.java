package com.windschief.spotify.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AlbumItem(
                String album_type,
                int total_tracks,
                List<String> available_markets,
                ExternalUrls external_urls,
                String href,
                String id,
                List<Image> images,
                String name,
                String release_date,
                String release_date_precision,
                Restrictions restrictions,
                String type,
                String uri,
                List<ArtistItem> artists,
                String album_group) {
}

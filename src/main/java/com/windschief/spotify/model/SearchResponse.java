package com.windschief.spotify.model;

public record SearchResponse(
        TracksResponse tracks,
        ArtistsResponse artists,
        AlbumsResponse albums,
        PlaylistsResponse playlists
) {}

package com.windschief.spotify.model;

import java.util.List;

public record PlaylistAddItemsRequest(List<String> uris, Integer position) {

}

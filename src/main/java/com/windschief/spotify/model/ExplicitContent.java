package com.windschief.spotify.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ExplicitContent {
    @JsonProperty("filter_enabled")
    private boolean filterEnabled;

    @JsonProperty("filter_locked")
    private boolean filterLocked;

    public boolean isFilterEnabled() {
        return filterEnabled;
    }

    public boolean isFilterLocked() {
        return filterLocked;
    }
}
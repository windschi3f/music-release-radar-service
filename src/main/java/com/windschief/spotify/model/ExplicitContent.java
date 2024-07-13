package com.windschief.spotify.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ExplicitContent(
        @JsonProperty("filter_enabled") boolean filterEnabled,
        @JsonProperty("filter_locked") boolean filterLocked
) {
}

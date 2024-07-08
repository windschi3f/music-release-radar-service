package com.windschief.spotify.model;

import java.util.List;
import com.fasterxml.jackson.annotation.JsonProperty;

public class SpotifyUser {
    private String id;
    @JsonProperty("display_name")
    private String displayName;
    private String email;
    private String country;
    @JsonProperty("exp licit_content")
    private ExplicitContent explicitContent;
    @JsonProperty("external_urls")
    private ExternalUrls externalUrls;
    private Followers followers;
    private String href;
    private List<Image> images;
    private String product;
    private String type;
    private String uri;

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getEmail() {
        return email;
    }

    public String getCountry() {
        return country;
    }

    public ExplicitContent getExplicitContent() {
        return explicitContent;
    }

    public ExternalUrls getExternalUrls() {
        return externalUrls;
    }

    public Followers getFollowers() {
        return followers;
    }

    public String getHref() {
        return href;
    }

    public List<Image> getImages() {
        return images;
    }

    public String getProduct() {
        return product;
    }

    public String getType() {
        return type;
    }

    public String getUri() {
        return uri;
    }
}

package com.windschief.spotify.model;

import java.util.List;

public class Artist {
    private ExternalUrls externalUrls;
    private Followers followers;
    private List<String> genres;
    private String href;
    private String id;
    private List<Image> images;
    private String name;
    private int popularity;
    private String type;
    private String uri;

    public ExternalUrls getExternalUrls() {
        return externalUrls;
    }

    public Followers getFollowers() {
        return followers;
    }

    public List<String> getGenres() {
        return genres;
    }

    public String getHref() {
        return href;
    }
    
    public String getId() {
        return id;
    }
    
    public List<Image> getImages() {
        return images;
    }
    
    public String getName() {
        return name;
    }
    
    public int getPopularity() {
        return popularity;
    }
    
    public String getType() {
        return type;
    }
    
    public String getUri() {
        return uri;
    }
}
package com.windschief.spotify.model;

import java.util.List;

public class Artists {
    private String href;
    private int limit;
    private String next;
    private Cursors cursors;
    private int total;
    private List<Artist> items;

    public String getHref() {
        return href;
    }

    public int getLimit() {
        return limit;
    }
    
    public String getNext() {
        return next;
    }
    
    public Cursors getCursors() {
        return cursors;
    }
    
    public int getTotal() {
        return total;
    }
    
    public List<Artist> getItems() {
        return items;
    }
}
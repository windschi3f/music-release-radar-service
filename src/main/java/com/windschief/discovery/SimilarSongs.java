package com.windschief.discovery;

import java.util.List;

public interface SimilarSongs {
    record Match(String artist, String title, double score) {}
    boolean available();
    List<Match> find(DiscoveryTrack seed);
}

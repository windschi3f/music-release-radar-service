package com.windschief.auth;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class SpotifyTokenRepository implements PanacheRepository<SpotifyToken> {
    public SpotifyToken findByUserId(String userId) {
        return find("userId", userId).firstResult();
    }
}
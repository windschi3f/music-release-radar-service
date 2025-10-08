package com.windschief.auth;

import java.util.Optional;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class SpotifyTokenRepository implements PanacheRepository<SpotifyToken> {
    public Optional<SpotifyToken> findByUserId(String userId) {
        return find("userId", userId).firstResultOptional();
    }

    public long deleteByUserId(String userId) {
        return delete("userId", userId);
    }
}
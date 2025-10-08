package com.windschief.task;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import io.quarkus.hibernate.orm.panache.PanacheRepository;

@ApplicationScoped
public class TaskRepository implements PanacheRepository<Task> {
    public List<Task> findByUserId(String userId) {
        return list("userId", userId);
    }

    public long countTasksForUser(String userId) {
        return count("userId = ?1", userId);
    }

    public long deleteByUserId(String userId) {
        return delete("userId", userId);
    }
}

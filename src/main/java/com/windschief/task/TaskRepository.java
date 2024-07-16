package com.windschief.task;

import java.util.List;

import com.windschief.task.domain.Task;

import jakarta.enterprise.context.ApplicationScoped;
import io.quarkus.hibernate.orm.panache.PanacheRepository;

@ApplicationScoped
public class TaskRepository implements PanacheRepository<Task> {
    public List<Task> findByUserId(String userId) {
        return list("userId", userId);
    }

    public long deleteByTaskIdAndUserId(Long taskId, String userId) {
        return delete("id = ?1 and userId = ?2", taskId, userId);
    }

    public long countTasksForUser(String userId) {
        return count("userId = ?1", userId);
    }
}

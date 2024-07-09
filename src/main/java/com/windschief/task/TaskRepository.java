package com.windschief.task;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;

import com.windschief.task.model.Task;
import io.quarkus.hibernate.orm.panache.PanacheRepository;

@ApplicationScoped
public class TaskRepository implements PanacheRepository<Task> {
    public List<Task> findByUserIdWithItems(String userId) {
        return list("from Task t left join fetch t.taskItems where t.userId = ?1", userId);
    }

    public long countTasksForUser(String userId) {
        return count("userId = ?1", userId);
    }
}

package com.windschief.task.item;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;

import io.quarkus.hibernate.orm.panache.PanacheRepository;

@ApplicationScoped
public class TaskItemRepository implements PanacheRepository<TaskItem> {
    public List<TaskItem> findByTaskId(Long taskId) {
        return list("task.id", taskId);
    }

    public List<TaskItem> findByUserId(String userId) {
        return list("task.userId", userId);
    }

    public long deleteByTaskIdAndTaskItemIdAndUserId(Long taskId, Long taskItemId, String userId) {
        return delete("task.id = ?1 and id = ?2 and task.userId = ?3", taskId, taskItemId, userId);
    }

    public long deleteByTaskId(Long taskId) {
        return delete("task.id", taskId);
    }

    public long deleteByUserId(String userId) {
        return delete("task.userId", userId);
    }
}

package com.windschief.task.added_item;

import java.util.List;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class AddedItemRepository implements PanacheRepository<AddedItem> {
    public List<AddedItem> findByTaskId(Long taskId) {
        return list("task.id", taskId);
    }

    public long deleteByTaskIdAndUserId(Long taskId, String userId) {
        return delete("task.id = ?1 and task.userId = ?2", taskId, userId);
    }
}
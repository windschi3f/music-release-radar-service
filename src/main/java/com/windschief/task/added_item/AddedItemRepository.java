package com.windschief.task.added_item;

import java.time.Instant;
import java.util.List;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class AddedItemRepository implements PanacheRepository<AddedItem> {
    public List<AddedItem> findByTaskId(Long taskId) {
        return list("task.id", taskId);
    }

    public boolean existsByTaskIdAndExternalIdAndItemType(Long taskId, String externalId, AddedItemType itemType) {
        return count("task.id = ?1 and externalId = ?2 and itemType = ?3", taskId, externalId, itemType) > 0;
    }

    public long deleteByTaskIdAndUserId(Long taskId, String userId) {
        return delete("task.id = ?1 and task.userId = ?2", taskId, userId);
    }

    public Instant getLastAddedAt(Long taskId) {
        AddedItem item = find("task.id = ?1 order by addedAt desc", taskId)
                .firstResult();
        return item != null ? item.getAddedAt() : null;
    }
}
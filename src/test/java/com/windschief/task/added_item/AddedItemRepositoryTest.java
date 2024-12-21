package com.windschief.task.added_item;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.windschief.task.Platform;
import com.windschief.task.Task;
import com.windschief.task.TaskRepository;

import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;

@QuarkusTest
class AddedItemRepositoryTest {

    @Inject
    AddedItemRepository addedItemRepository;

    @Inject
    TaskRepository taskRepository;

    @BeforeEach
    @TestTransaction
    void setUp() {
        List<AddedItem> items = addedItemRepository.listAll();
        for (AddedItem item : items) {
            addedItemRepository.delete(item);
        }
        List<Task> tasks = taskRepository.listAll();
        for (Task task : tasks) {
            taskRepository.delete(task);
        }
    }

    @Test
    @TestTransaction
    void givenPersistedAddedItem_whenFindByTaskId_thenItemIsFound() {
        // GIVEN
        Task task = new Task();
        task.setUserId("user");
        task.setPlatform(Platform.SPOTIFY);
        taskRepository.persist(task);

        AddedItem addedItem = new AddedItem();
        addedItem.setTask(task);
        addedItem.setExternalId("spotify:track:123");
        addedItem.setTitle("Test Track");
        addedItem.setAddedAt(Instant.now());
        addedItemRepository.persist(addedItem);

        // WHEN
        List<AddedItem> foundItems = addedItemRepository.findByTaskId(task.getId());

        // THEN
        assertEquals(1, foundItems.size());
        assertEquals("spotify:track:123", foundItems.get(0).getExternalId());
        assertEquals("Test Track", foundItems.get(0).getTitle());
    }

    @Test
    @TestTransaction
    void givenPersistedAddedItem_whenDeleteByTaskIdAndUserId_thenItemIsDeleted() {
        // GIVEN
        Task task = new Task();
        task.setUserId("user");
        task.setPlatform(Platform.SPOTIFY);
        taskRepository.persist(task);

        AddedItem addedItem = new AddedItem();
        addedItem.setTask(task);
        addedItem.setExternalId("spotify:track:123");
        addedItemRepository.persist(addedItem);

        // WHEN
        long deletedCount = addedItemRepository.deleteByTaskIdAndUserId(task.getId(), "user");

        // THEN
        assertEquals(1, deletedCount);
        assertTrue(addedItemRepository.findByTaskId(task.getId()).isEmpty());
    }
}
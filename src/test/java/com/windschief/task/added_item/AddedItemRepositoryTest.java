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
        addedItem.setAddedAt(Instant.now());
        addedItemRepository.persist(addedItem);

        // WHEN
        List<AddedItem> foundItems = addedItemRepository.findByTaskId(task.getId());

        // THEN
        assertEquals(1, foundItems.size());
        assertEquals("spotify:track:123", foundItems.get(0).getExternalId());
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

    @Test
    @TestTransaction
    void givenNoAddedItems_whenGetLastAddedAt_thenReturnsNull() {
        // GIVEN
        Task task = new Task();
        task.setUserId("user");
        task.setPlatform(Platform.SPOTIFY);
        taskRepository.persist(task);

        // WHEN
        Instant lastAddedAt = addedItemRepository.getLastAddedAt(task.getId());

        // THEN
        assertEquals(null, lastAddedAt);
    }

    @Test
    @TestTransaction
    void givenSingleAddedItem_whenGetLastAddedAt_thenReturnsItemTimestamp() {
        // GIVEN
        Task task = new Task();
        task.setUserId("user");
        task.setPlatform(Platform.SPOTIFY);
        taskRepository.persist(task);

        Instant timestamp = Instant.now();
        AddedItem addedItem = new AddedItem();
        addedItem.setTask(task);
        addedItem.setExternalId("spotify:track:123");
        addedItem.setAddedAt(timestamp);
        addedItemRepository.persist(addedItem);

        // WHEN
        Instant lastAddedAt = addedItemRepository.getLastAddedAt(task.getId());

        // THEN
        assertEquals(timestamp, lastAddedAt);
    }

    @Test
    @TestTransaction
    void givenMultipleAddedItems_whenGetLastAddedAt_thenReturnsLatestTimestamp() {
        // GIVEN
        Task task = new Task();
        task.setUserId("user");
        task.setPlatform(Platform.SPOTIFY);
        taskRepository.persist(task);

        Instant earlierTimestamp = Instant.now().minusSeconds(3600);
        AddedItem earlierItem = new AddedItem();
        earlierItem.setTask(task);
        earlierItem.setExternalId("spotify:track:123");
        earlierItem.setAddedAt(earlierTimestamp);
        addedItemRepository.persist(earlierItem);

        Instant laterTimestamp = Instant.now();
        AddedItem laterItem = new AddedItem();
        laterItem.setTask(task);
        laterItem.setExternalId("spotify:track:456");
        laterItem.setAddedAt(laterTimestamp);
        addedItemRepository.persist(laterItem);

        // WHEN
        Instant lastAddedAt = addedItemRepository.getLastAddedAt(task.getId());

        // THEN
        assertEquals(laterTimestamp, lastAddedAt);
    }

    @Test
    @TestTransaction
    void givenNoAddedItems_whenExistsByExternalReferenceIdAndTaskId_thenReturnsFalse() {
        // GIVEN
        Task task = new Task();
        task.setUserId("user");
        task.setPlatform(Platform.SPOTIFY);
        taskRepository.persist(task);

        // WHEN
        boolean exists = addedItemRepository.existsByExternalIdAndTaskId("spotify:track:123", task.getId());

        // THEN
        assertEquals(false, exists);
    }

    @Test
    @TestTransaction
    void givenAddedItem_whenExistsByExternalReferenceIdAndTaskId_thenReturnsTrue() {
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
        boolean exists = addedItemRepository.existsByExternalIdAndTaskId("spotify:track:123", task.getId());

        // THEN
        assertEquals(true, exists);
    }
}
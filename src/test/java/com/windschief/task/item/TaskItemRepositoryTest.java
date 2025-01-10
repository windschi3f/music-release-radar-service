package com.windschief.task.item;

import java.util.List;

import jakarta.inject.Inject;

import com.windschief.task.Platform;
import com.windschief.task.Task;
import com.windschief.task.TaskRepository;

import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@QuarkusTest
class TaskItemRepositoryTest {

    @Inject
    TaskItemRepository taskItemRepository;

    @Inject
    TaskRepository taskRepository;

    @Test
    @TestTransaction
    void givenPersistedTaskItemForTask_whenFindByTaskId_thenTaskItemIsFound() {
        // GIVEN
        Task task = new Task();
        task.setUserId("user");
        task.setPlatform(Platform.YOUTUBE);

        TaskItem taskItem = new TaskItem();
        taskItem.setItemType(TaskItemType.ARTIST);
        taskItem.setExternalReferenceId("1");
        task.addTaskItem(taskItem);

        taskRepository.persist(task);

        // WHEN
        List<TaskItem> foundTaskItems = taskItemRepository.findByTaskId(task.getId());

        // THEN
        assertNotNull(taskItem.getId(), "TaskItem ID should not be null after persisting");
        assertEquals(1, foundTaskItems.size());
        assertEquals(TaskItemType.ARTIST, foundTaskItems.get(0).getItemType());
    }

    @Test
    @TestTransaction
    void givenPersistedTaskWithItem_whenDeleteByTaskIdAndTaskItemIdAndUserId_thenTaskIsDeleted() {
        // GIVEN
        Task task = new Task();
        task.setUserId("user");
        task.setPlatform(Platform.YOUTUBE);

        TaskItem taskItem = new TaskItem();
        taskItem.setItemType(TaskItemType.ARTIST);
        taskItem.setExternalReferenceId("1");
        task.addTaskItem(taskItem);

        taskRepository.persist(task);

        // WHEN
        long deletedCount = taskItemRepository.deleteByTaskIdAndTaskItemIdAndUserId(task.getId(), taskItem.getId(),
                task.getUserId());

        // THEN
        assertEquals(1, deletedCount);
        assertEquals(0, taskItemRepository.findByTaskId(task.getId()).size());
    }

    @Test
    @TestTransaction
    void givenTaskAndItem_whenPersistTaskItem_thenTaskItemIsPersisted() {
        // GIVEN
        Task task = new Task();
        task.setUserId("user");
        task.setPlatform(Platform.YOUTUBE);
        taskRepository.persist(task);

        TaskItem taskItem = new TaskItem();
        taskItem.setItemType(TaskItemType.ARTIST);
        taskItem.setExternalReferenceId("1");
        taskItem.setTask(task);

        // WHEN
        taskItemRepository.persist(taskItem);

        // THEN
        assertNotNull(taskItem.getId(), "TaskItem ID should not be null after persisting");
    }
}
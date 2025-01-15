package com.windschief.task;

import java.util.List;
import java.util.Optional;

import jakarta.inject.Inject;

import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import org.hibernate.Hibernate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.windschief.task.item.TaskItem;
import com.windschief.task.item.TaskItemRepository;
import com.windschief.task.item.TaskItemType;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
class TaskRepositoryTest {
    @Inject
    TaskRepository taskRepository;
    @Inject
    TaskItemRepository taskItemRepository;

    @BeforeEach
    @TestTransaction
    void setUp() {
        List<Task> tasks = taskRepository.listAll();
        for (Task task : tasks) {
            taskRepository.delete(task);
        }
    }

    @Test
    @TestTransaction
    void givenPersistedTask_whenFindByIdOptional_thenTaskIsFound() {
        // GIVEN
        Task task = new Task();
        task.setUserId("user");
        task.setActive(true);
        task.setPlatform(Platform.SPOTIFY);

        taskRepository.persist(task);

        // WHEN
        Optional<Task> foundTask = taskRepository.findByIdOptional(task.getId());

        // THEN
        assertNotNull(task.getId(), "Task ID should not be null after persisting");
        assertTrue(foundTask.isPresent());
        assertEquals("user", foundTask.get().getUserId());
    }

    @Test
    @TestTransaction
    void givenPersistedTaskWithItems_whenFindByUserId_thenTaskIsFound() {
        // GIVEN
        Task task = new Task();
        task.setUserId("user");
        task.setPlatform(Platform.YOUTUBE);

        TaskItem item = new TaskItem();
        item.setItemType(TaskItemType.ARTIST);
        item.setExternalReferenceId("1");
        task.addTaskItem(item);

        taskRepository.persist(task);

        // WHEN
        List<Task> tasks = taskRepository.findByUserId("user");

        // THEN
        assertEquals(1, tasks.size());
        assertEquals(1, tasks.get(0).getTaskItems().size());
        assertEquals("1", tasks.get(0).getTaskItems().get(0).getExternalReferenceId());
    }

    @Test
    @TestTransaction
    void givenPersistedTaskWithItems_whenRetrievedWithinSameTransaction_thenTaskItemsAreLazilyLoadedWithAdditionalQuery() {
        // GIVEN
        Task task = new Task();
        task.setUserId("user");
        task.setPlatform(Platform.YOUTUBE);

        TaskItem item = new TaskItem();
        item.setItemType(TaskItemType.ARTIST);
        item.setExternalReferenceId("1");
        task.addTaskItem(item);

        taskRepository.persist(task);

        // Simulate detaching entities
        taskRepository.getEntityManager().flush();
        taskRepository.getEntityManager().clear();

        // WHEN
        Task taskRetrieved = taskRepository.findById(task.getId());

        // THEN
        assertFalse(Hibernate.isInitialized(taskRetrieved.getTaskItems()),
                "TaskItems should not be initialized upon Task retrieval");
        assertEquals(1, taskRetrieved.getTaskItems().size(),
                "TaskItems should be accessible within the same transaction");
    }

    @Test
    @TestTransaction
    void givenUpdatedActiveStatus_whenGetUpdatedTask_thenActiveStatusIsUpdated() {
        // GIVEN
        Task task = new Task();
        task.setActive(true);
        taskRepository.persist(task);

        task.setActive(false);
        taskRepository.persist(task);

        // WHEN
        Task updatedTask = taskRepository.findById(task.getId());

        // THEN
        assertFalse(updatedTask.isActive(), "Updated task should not be active");
    }

    @Test
    @TestTransaction
    void givenPersistedTaskForUser_whenCountTasksForUser_thenCountIsCorrect() {
        // GIVEN
        Task task1 = new Task();
        task1.setUserId("user");

        Task task2 = new Task();
        task2.setUserId("user");

        taskRepository.persist(task1);
        taskRepository.persist(task2);

        // WHEN
        long count = taskRepository.countTasksForUser("user");

        // THEN
        assertEquals(2, count);
    }

    @Test
    @TestTransaction
    void givenPersistedTaskWithItems_whenDeleteTask_thenTaskAndItemsAreDeleted() {
        // GIVEN
        Task task = new Task();
        task.setUserId("user");
        task.setPlatform(Platform.YOUTUBE);
        task.addTaskItem(new TaskItem());

        taskRepository.persist(task);

        assertEquals(1, taskItemRepository.listAll().size());

        // WHEN
        taskRepository.delete(task);

        // THEN
        assertEquals(0, taskRepository.findByUserId("user").size());
        assertEquals(0, taskItemRepository.listAll().size());
    }
}
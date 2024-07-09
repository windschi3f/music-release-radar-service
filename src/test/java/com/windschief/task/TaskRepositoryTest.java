package com.windschief.task;

import java.util.List;
import java.util.Optional;

import jakarta.inject.Inject;

import com.windschief.task.model.Platform;
import com.windschief.task.model.Task;
import com.windschief.task.model.TaskItem;
import com.windschief.task.model.TaskItemType;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
class TaskRepositoryTest {

    @Inject
    TaskRepository taskRepository;

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
    void givenPersistedTaskWithItems_whenFindByUserIdWithItems_thenTaskIsFound() {
        // GIVEN
        Task task = new Task();
        task.setUserId("user");
        task.setPlatform(Platform.YOUTUBE);

        TaskItem item = new TaskItem();
        item.setItemType(TaskItemType.ARTIST);
        item.setItemId("1");
        task.addTaskItem(item);

        taskRepository.persist(task);

        // WHEN
        List<Task> tasks = taskRepository.findByUserIdWithItems("user");

        // THEN
        assertEquals(1, tasks.size());
        assertEquals(1, tasks.get(0).getTaskItems().size());
        assertEquals("1", tasks.get(0).getTaskItems().get(0).getItemId());
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
}
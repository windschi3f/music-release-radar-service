package com.windschief.task.service;

import java.security.Principal;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;

import jakarta.ws.rs.core.Response;

import com.windschief.task.TaskRepository;
import com.windschief.task.domain.Platform;
import com.windschief.task.domain.Task;
import com.windschief.task.dto.TaskRequestDto;
import com.windschief.task.dto.TaskResponseDto;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@QuarkusTest
class TaskServiceTest {

    private final TaskRepository taskRepository = mock(TaskRepository.class);
    private final SecurityIdentity securityIdentity = mock(SecurityIdentity.class);
    private final Principal principal = mock(Principal.class);

    private TaskService taskService;

    @BeforeEach
    public void setup() {
        taskService = new TaskService(securityIdentity, taskRepository);

        when(securityIdentity.getPrincipal()).thenReturn(principal);
        when(principal.getName()).thenReturn("testUser");
    }

    @Test
    void givenExistingTasks_whenGetTasksIsCalled_thenAllUserTasksAreReturned() {
        // Given
        Task task1 = new Task();
        Task task2 = new Task();
        when(taskRepository.findByUserId("testUser")).thenReturn(Arrays.asList(task1, task2));

        // When
        List<TaskResponseDto> result = taskService.getTasks();

        // Then
        assertEquals(2, result.size());
        verify(taskRepository).findByUserId("testUser");
    }

    @Test
    void givenExistingTask_whenGetTaskIsCalled_thenTaskIsReturned() {
        // Given
        Long taskId = 1L;
        Task task = new Task();
        task.setUserId("testUser");
        when(taskRepository.findById(taskId)).thenReturn(task);

        // When
        Response response = taskService.getTask(taskId);

        // Then
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    }

    @Test
    void givenNonExistentTask_whenGetTaskIsCalled_thenResponseIsNotFound() {
        // Given
        Long taskId = 1L;
        when(taskRepository.findById(taskId)).thenReturn(null);

        // When
        Response response = taskService.getTask(taskId);

        // Then
        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());
    }

    @Test
    void givenTaskOwnedByDifferentUser_whenGetTaskIsCalled_thenResponseIsUnauthorized() {
        // Given
        Long taskId = 1L;
        Task task = new Task();
        task.setUserId("otherUser");
        when(taskRepository.findById(taskId)).thenReturn(task);

        // When
        Response response = taskService.getTask(taskId);

        // Then
        assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), response.getStatus());
    }

    @Test
    void givenValidTaskRequest_whenCreateTaskIsCalled_thenTaskIsCreated() {
        // Given
        TaskRequestDto taskRequestDto = new TaskRequestDto(Platform.SPOTIFY, 7, Instant.now(), true);

        doAnswer(invocation -> {
            Task task = invocation.getArgument(0);
            task.setId(1L);
            return null;
        }).when(taskRepository).persist(any(Task.class));

        // When
        TaskResponseDto result = taskService.createTask(taskRequestDto);

        // Then
        assertNotNull(result);
        verify(taskRepository).persist(any(Task.class));
    }

    @Test
    void givenExistingTask_whenUpdateTaskIsCalled_thenTaskIsUpdated() {
        // Given
        Long taskId = 1L;
        Task existingTask = new Task();
        existingTask.setUserId("testUser");
        when(taskRepository.findById(taskId)).thenReturn(existingTask);

        TaskRequestDto taskRequestDto = new TaskRequestDto(Platform.SPOTIFY, 7, Instant.now(), true);

        // When
        Response response = taskService.updateTask(taskId, taskRequestDto);

        // Then
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        verify(taskRepository).persist(any(Task.class));
    }

    @Test
    void givenNonExistentTask_whenUpdateTaskIsCalled_thenResponseIsNotFound() {
        // Given
        Long taskId = 1L;
        when(taskRepository.findById(taskId)).thenReturn(null);

        TaskRequestDto taskRequestDto = new TaskRequestDto(Platform.SPOTIFY, 7, Instant.now(), true);

        // When
        Response response = taskService.updateTask(taskId, taskRequestDto);

        // Then
        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());
    }

    @Test
    void givenTaskOwnedByDifferentUser_whenUpdateTaskIsCalled_thenResponseIsUnauthorized() {
        // Given
        Long taskId = 1L;
        Task existingTask = new Task();
        existingTask.setUserId("otherUser");
        when(taskRepository.findById(taskId)).thenReturn(existingTask);

        TaskRequestDto taskRequestDto = new TaskRequestDto(Platform.SPOTIFY, 7, Instant.now(), true);

        // When
        Response response = taskService.updateTask(taskId, taskRequestDto);

        // Then
        assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), response.getStatus());
    }

    @Test
    void givenExistingTask_whenDeleteTaskIsCalled_thenTaskIsDeletedAndResponseIsNoContent() {
        // Given
        Long taskId = 1L;
        when(taskRepository.deleteByTaskIdAndUserId(taskId, "testUser")).thenReturn(1L);

        // When
        Response response = taskService.deleteTask(taskId);

        // Then
        assertEquals(Response.Status.NO_CONTENT.getStatusCode(), response.getStatus());
    }

    @Test
    void givenNonExistentTask_whenDeleteTaskIsCalled_thenResponseIsNotFound() {
        // Given
        Long taskId = 1L;
        when(taskRepository.deleteByTaskIdAndUserId(taskId, "testUser")).thenReturn(0L);

        // When
        Response response = taskService.deleteTask(taskId);

        // Then
        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());
    }
}
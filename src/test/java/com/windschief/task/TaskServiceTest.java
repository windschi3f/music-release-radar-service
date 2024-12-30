package com.windschief.task;

import java.security.Principal;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import jakarta.ws.rs.core.Response;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.windschief.auth.SpotifyTokenService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@QuarkusTest
class TaskServiceTest {
    private final TaskAccess taskAccess = mock(TaskAccess.class);
    private final TaskRepository taskRepository = mock(TaskRepository.class);
    private final SpotifyTokenService spotifyTokenService = mock(SpotifyTokenService.class);
    private TaskService taskService;

    @BeforeEach
    public void setup() {
        taskService = new TaskService(taskAccess, taskRepository, spotifyTokenService);
        when(taskAccess.getCurrentUserId()).thenReturn("testUser");

        final Principal principal = mock(Principal.class);
        when(principal.getName()).thenReturn("testUser");

        final SecurityIdentity securityIdentity = mock(SecurityIdentity.class);
        when(securityIdentity.getPrincipal()).thenReturn(principal);
        when(securityIdentity.getAttribute("spotifyToken")).thenReturn("token");

        when(taskAccess.getSecurityIdentity()).thenReturn(securityIdentity);
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
        when(taskAccess.checkAccess(task)).thenReturn(Optional.empty());

        // When
        Response response = taskService.getTask(taskId);

        // Then
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    }

    @Test
    void givenNonExistentTask_whenGetTaskIsCalled_thenResponseIsNotFound() {
        // Given
        Long taskId = 1L;
        Task task = null;
        when(taskRepository.findById(taskId)).thenReturn(task);
        when(taskAccess.checkAccess(task)).thenReturn(
                Optional.of(Response.status(Response.Status.NOT_FOUND).build()));

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
        when(taskAccess.checkAccess(task)).thenReturn(
                Optional.of(Response.status(Response.Status.UNAUTHORIZED).build()));

        // When
        Response response = taskService.getTask(taskId);

        // Then
        assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), response.getStatus());
    }

    @Test
    void givenValidTaskRequest_whenCreateTaskIsCalled_thenTaskIsCreated() {
        // Given
        TaskRequestDto taskRequestDto = new TaskRequestDto(Platform.SPOTIFY, 7, Instant.now(), true, "refreshToken");

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
        verify(spotifyTokenService).updateStoredToken("testUser", "token", "refreshToken");
    }

    @Test
    void givenMissingRefreshToken_whenCreateTaskIsCalled_thenExceptionIsThrown() {
        // GIVEN
        TaskRequestDto taskRequestDto = new TaskRequestDto(Platform.SPOTIFY, 7, Instant.now(), true,
                null);

        // WHEN
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> taskService.createTask(taskRequestDto));

        // THEN
        assertEquals("refreshToken is required", exception.getMessage());
    }

    @Test
    void givenExistingTask_whenUpdateTaskIsCalled_thenTaskIsUpdated() {
        // Given
        Long taskId = 1L;
        Task existingTask = new Task();
        existingTask.setUserId("testUser");
        when(taskRepository.findById(taskId)).thenReturn(existingTask);
        when(taskAccess.checkAccess(existingTask)).thenReturn(Optional.empty());

        TaskRequestDto taskRequestDto = new TaskRequestDto(Platform.SPOTIFY, 7, Instant.now(), true,
                "refreshToken");

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
        Task task = null;
        when(taskRepository.findById(taskId)).thenReturn(task);
        when(taskAccess.checkAccess(task)).thenReturn(
                Optional.of(Response.status(Response.Status.NOT_FOUND).build()));

        TaskRequestDto taskRequestDto = new TaskRequestDto(Platform.SPOTIFY, 7, Instant.now(), true,
                "refreshToken");

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
        when(taskAccess.checkAccess(existingTask)).thenReturn(
                Optional.of(Response.status(Response.Status.UNAUTHORIZED).build()));

        TaskRequestDto taskRequestDto = new TaskRequestDto(Platform.SPOTIFY, 7, Instant.now(), true,
                "refreshToken");

        // When
        Response response = taskService.updateTask(taskId, taskRequestDto);

        // Then
        assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), response.getStatus());
    }

    @Test
    void givenExistingTask_whenDeleteTaskIsCalled_thenTaskIsDeletedAndResponseIsNoContent() {
        // Given
        Long taskId = 1L;
        Task task = new Task();
        when(taskRepository.findById(taskId)).thenReturn(task);
        when(taskAccess.checkAccess(task)).thenReturn(Optional.empty());
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
        Task task = new Task();
        when(taskRepository.findById(taskId)).thenReturn(task);
        when(taskAccess.checkAccess(task)).thenReturn(Optional.empty());
        when(taskRepository.deleteByTaskIdAndUserId(taskId, "testUser")).thenReturn(0L);

        // When
        Response response = taskService.deleteTask(taskId);

        // Then
        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());
    }
}
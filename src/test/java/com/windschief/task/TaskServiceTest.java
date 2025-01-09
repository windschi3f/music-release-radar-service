package com.windschief.task;

import java.security.Principal;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;

import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.windschief.auth.SpotifyTokenService;
import com.windschief.releasedetection.ReleaseRadarService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@QuarkusTest
class TaskServiceTest {
    private final TaskAccess taskAccess = mock(TaskAccess.class);
    private final TaskRepository taskRepository = mock(TaskRepository.class);
    private final SpotifyTokenService spotifyTokenService = mock(SpotifyTokenService.class);
    private final ReleaseRadarService releaseRadarService = mock(ReleaseRadarService.class);
    private final TaskService taskService = new TaskService(taskAccess, taskRepository, spotifyTokenService,
            releaseRadarService);

    @BeforeEach
    public void setup() {
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
        // GIVEN
        Task task1 = new Task();
        Task task2 = new Task();
        when(taskRepository.findByUserId("testUser")).thenReturn(Arrays.asList(task1, task2));

        // WHEN
        List<TaskResponseDto> result = taskService.getTasks();

        // THEN
        assertEquals(2, result.size());
        verify(taskRepository).findByUserId("testUser");
    }

    @Test
    void givenExistingTask_whenGetTaskIsCalled_thenTaskIsReturned() {
        // GIVEN
        Long taskId = 1L;
        Task task = new Task();
        task.setUserId("testUser");
        when(taskRepository.findById(taskId)).thenReturn(task);

        // WHEN
        Response response = taskService.getTask(taskId);

        // THEN
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    }

    @Test
    void givenTaskAccessException_whenGetTaskIsCalled_thenThrowException() {
        // GIVEN
        Long taskId = 1L;
        Task task = new Task();
        task.setUserId("otherUser");
        when(taskRepository.findById(taskId)).thenReturn(task);
        doThrow(new NotAuthorizedException("Thrown on purpose")).when(taskAccess).checkAccess(task);

        // WHEN / THEN
        assertThrows(NotAuthorizedException.class, () -> taskService.getTask(taskId));
    }

    @Test
    void givenValidTaskRequest_whenCreateTaskIsCalled_thenTaskIsCreated() {
        // GIVEN
        TaskRequestDto taskRequestDto = new TaskRequestDto("test", Platform.SPOTIFY, 7, Instant.now(), true, "123",
                "refreshToken");

        doAnswer(invocation -> {
            Task task = invocation.getArgument(0);
            task.setId(1L);
            return null;
        }).when(taskRepository).persist(any(Task.class));

        // WHEN
        TaskResponseDto result = taskService.createTask(taskRequestDto);

        // THEN
        assertNotNull(result);
        verify(taskRepository).persist(any(Task.class));
        verify(spotifyTokenService).updateStoredToken("testUser", "token", "refreshToken");
    }

    @Test
    void givenMissingRefreshToken_whenCreateTaskIsCalled_thenExceptionIsThrown() {
        // GIVEN
        TaskRequestDto taskRequestDto = new TaskRequestDto("test", Platform.SPOTIFY, 7, Instant.now(), true, "123",
                null);

        // WHEN
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> taskService.createTask(taskRequestDto));

        // THEN
        assertEquals("refreshToken is required", exception.getMessage());
    }

    @Test
    void givenExistingTask_whenUpdateTaskIsCalled_thenTaskIsUpdated() {
        // GIVEN
        Long taskId = 1L;
        Task existingTask = new Task();
        existingTask.setUserId("testUser");
        when(taskRepository.findById(taskId)).thenReturn(existingTask);

        TaskRequestDto taskRequestDto = new TaskRequestDto("test", Platform.SPOTIFY, 7, Instant.now(), true, "123",
                "refreshToken");

        // WHEN
        Response response = taskService.updateTask(taskId, taskRequestDto);

        // THEN
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        assertEquals(Platform.SPOTIFY, existingTask.getPlatform());
        assertEquals(7, existingTask.getExecutionIntervalDays());
        assertEquals("123", existingTask.getPlaylistId());
        assertTrue(existingTask.isActive());
    }

    @Test
    void givenTaskAccessException_whenUpdateTaskIsCalled_thenThrowException() {
        // GIVEN
        Long taskId = 1L;
        Task task = null;
        when(taskRepository.findById(taskId)).thenReturn(task);
        doThrow(new NotFoundException()).when(taskAccess).checkAccess(task);

        TaskRequestDto taskRequestDto = new TaskRequestDto("test", Platform.SPOTIFY, 7, Instant.now(), true, "123",
                "refreshToken");

        // WHEN / THEN
        assertThrows(NotFoundException.class, () -> taskService.updateTask(taskId, taskRequestDto));
    }

    @Test
    void givenExistingTask_whenDeleteTaskIsCalled_thenTaskIsDeletedAndResponseIsNoContent() {
        // GIVEN
        Long taskId = 1L;
        Task task = new Task();
        when(taskRepository.findById(taskId)).thenReturn(task);
        when(taskRepository.deleteByTaskIdAndUserId(taskId, "testUser")).thenReturn(1L);

        // WHEN
        Response response = taskService.deleteTask(taskId);

        // THEN
        assertEquals(Response.Status.NO_CONTENT.getStatusCode(), response.getStatus());
    }

    @Test
    void givenNonExistentTask_whenDeleteTaskIsCalled_thenResponseIsNotFound() {
        // GIVEN
        Long taskId = 1L;
        Task task = new Task();
        when(taskRepository.findById(taskId)).thenReturn(task);
        when(taskRepository.deleteByTaskIdAndUserId(taskId, "testUser")).thenReturn(0L);

        // WHEN
        Response response = taskService.deleteTask(taskId);

        // THEN
        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());
    }
}
package com.windschief.task;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.security.Principal;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.ws.rs.core.Response;

@QuarkusTest
class TaskAccessTest {
    private final SecurityIdentity securityIdentity = mock(SecurityIdentity.class);
    private final TaskRepository taskRepository = mock(TaskRepository.class);
    private final Principal principal = mock(Principal.class);
    private TaskAccess taskAccess;

    @BeforeEach
    void setup() {
        taskAccess = new TaskAccess(securityIdentity, taskRepository);
        when(securityIdentity.getPrincipal()).thenReturn(principal);
        when(principal.getName()).thenReturn("testUser");
    }

    @Test
    void whenTaskIsNull_thenReturnNotFound() {
        // GIVEN
        Task task = null;

        // WHEN
        Optional<Response> result = taskAccess.checkAccess(task);

        // THEN
        assertTrue(result.isPresent());
        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), result.get().getStatus());
    }

    @Test
    void whenTaskBelongsToUser_thenReturnEmpty() {
        // GIVEN
        Task task = new Task();
        task.setUserId("testUser");

        // WHEN
        Optional<Response> result = taskAccess.checkAccess(task);

        // THEN
        assertFalse(result.isPresent());
    }

    @Test
    void whenTaskBelongsToOtherUser_thenReturnUnauthorized() {
        // GIVEN
        Task task = new Task();
        task.setUserId("otherUser");

        // WHEN
        Optional<Response> result = taskAccess.checkAccess(task);

        // THEN
        assertTrue(result.isPresent());
        assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), result.get().getStatus());
    }

    @Test
    void whenCheckingByTaskId_andTaskDoesNotExist_thenReturnNotFound() {
        // GIVEN
        Long taskId = 1L;
        when(taskRepository.findById(taskId)).thenReturn(null);

        // WHEN
        Optional<Response> result = taskAccess.checkAccess(taskId);

        // THEN
        assertTrue(result.isPresent());
        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), result.get().getStatus());
    }

    @Test
    void whenCheckingByTaskId_andTaskBelongsToUser_thenReturnEmpty() {
        // GIVEN
        Long taskId = 1L;
        Task task = new Task();
        task.setUserId("testUser");
        when(taskRepository.findById(taskId)).thenReturn(task);

        // WHEN
        Optional<Response> result = taskAccess.checkAccess(taskId);

        // THEN
        assertFalse(result.isPresent());
    }

    @Test
    void whenCheckingByTaskId_andTaskBelongsToOtherUser_thenReturnUnauthorized() {
        // GIVEN
        Long taskId = 1L;
        Task task = new Task();
        task.setUserId("otherUser");
        when(taskRepository.findById(taskId)).thenReturn(task);

        // WHEN
        Optional<Response> result = taskAccess.checkAccess(taskId);

        // THEN
        assertTrue(result.isPresent());
        assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), result.get().getStatus());
    }

    @Test
    void getCurrentUserIdReturnsCorrectUserId() {
        // WHEN
        String userId = taskAccess.getCurrentUserId();

        // THEN
        assertEquals("testUser", userId);
    }
}
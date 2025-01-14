package com.windschief.task;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.security.Principal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.security.identity.SecurityIdentity;
import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.NotFoundException;

class TaskAccessTest {
    private final SecurityIdentity securityIdentity = mock(SecurityIdentity.class);
    private final TaskRepository taskRepository = mock(TaskRepository.class);
    private final Principal principal = mock(Principal.class);
    private final TaskAccess taskAccess = new TaskAccess(securityIdentity, taskRepository);

    @BeforeEach
    void setup() {
        when(securityIdentity.getPrincipal()).thenReturn(principal);
        when(principal.getName()).thenReturn("testUser");
    }

    @Test
    void givenNullTask_whenCheckAccess_thenThrowNotFoundException() {
        // GIVEN
        Task task = null;

        // WHEN / THEN
        assertThrows(NotFoundException.class, () -> taskAccess.checkAccess(task));
    }

    @Test
    void givenTaskBelongsToUser_whenCheckAccess_thenThrowNothing() {
        // GIVEN
        Task task = new Task();
        task.setUserId("testUser");

        // WHEN / THEN
        taskAccess.checkAccess(task);
    }

    @Test
    void givenTaskBelongsToAnotherUser_whenCheckAccess_thenThrowNotAuthorizedException() {
        // GIVEN
        Task task = new Task();
        task.setUserId("otherUser");

        // WHEN / THEN
        assertThrows(NotAuthorizedException.class, () -> taskAccess.checkAccess(task));
    }

    @Test
    void givenNonExistingTaskId_whenCheckAccess_thenThrowNotFoundException() {
        // GIVEN
        Long taskId = 1L;
        when(taskRepository.findById(taskId)).thenReturn(null);

        // WHEN / THEN
        assertThrows(NotFoundException.class, () -> taskAccess.checkAccess(taskId));
    }

    @Test
    void givenTaskIdBelongingToUser_whenCheckAccess_thenNoExceptionThrown() {
        // GIVEN
        Long taskId = 1L;
        Task task = new Task();
        task.setUserId("testUser");
        when(taskRepository.findById(taskId)).thenReturn(task);

        // WHEN / THEN
        taskAccess.checkAccess(taskId);
    }

    @Test
    void givenTaskIdBelongingToAnotherUser_whenCheckAccess_thenThrowNotAuthorizedException() {
        // GIVEN
        Long taskId = 1L;
        Task task = new Task();
        task.setUserId("otherUser");
        when(taskRepository.findById(taskId)).thenReturn(task);

        // WHEN / THEN
        assertThrows(NotAuthorizedException.class, () -> taskAccess.checkAccess(taskId));
    }
}
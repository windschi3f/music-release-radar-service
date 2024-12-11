package com.windschief.task;

import java.security.Principal;
import java.util.Arrays;
import java.util.List;

import jakarta.ws.rs.core.Response;

import com.windschief.task.item.TaskItem;
import com.windschief.task.item.TaskItemRequestDto;
import com.windschief.task.item.TaskItemResponseDto;
import com.windschief.task.item.TaskItemService;
import com.windschief.task.item.TaskItemType;

import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@QuarkusTest
class TaskItemServiceTest {

    private final TaskRepository taskRepository = mock(TaskRepository.class);
    private final TaskItemRepository taskItemRepository = mock(TaskItemRepository.class);
    private final SecurityIdentity securityIdentity = mock(SecurityIdentity.class);
    private final Principal principal = mock(Principal.class);

    private TaskItemService taskItemService;

    @BeforeEach
    public void setup() {
        taskItemService = new TaskItemService(securityIdentity, taskRepository, taskItemRepository);

        when(securityIdentity.getPrincipal()).thenReturn(principal);
        when(principal.getName()).thenReturn("testUser");
    }

    @Test
    void givenExistingTaskItems_whenGetTaskItemsIsCalled_thenAllTaskItemsAreReturned() {
        // Given
        Long taskId = 1L;
        TaskItem item1 = new TaskItem();
        TaskItem item2 = new TaskItem();
        when(taskItemRepository.findByTaskId(taskId)).thenReturn(Arrays.asList(item1, item2));

        // When
        List<TaskItemResponseDto> result = taskItemService.getTaskItems(taskId);

        // Then
        assertEquals(2, result.size());
        verify(taskItemRepository).findByTaskId(taskId);
    }

    @Test
    void givenValidTaskAndTaskItems_whenCreateTaskItemsIsCalled_thenTaskItemsAreCreatedAndResponseIsCreated() {
        // Given
        Long taskId = 1L;
        Task task = new Task();
        task.setUserId("testUser");
        when(taskRepository.findById(taskId)).thenReturn(task);

        TaskItemRequestDto dto1 = new TaskItemRequestDto(TaskItemType.PLAYLIST, "1");
        TaskItemRequestDto dto2 = new TaskItemRequestDto(TaskItemType.PLAYLIST, "2");
        List<TaskItemRequestDto> dtos = Arrays.asList(dto1, dto2);

        // When
        Response response = taskItemService.createTaskItems(taskId, dtos);

        // Then
        assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());
        verify(taskItemRepository).persist(anyList());
    }

    @Test
    void givenNonExistentTask_whenCreateTaskItemsIsCalled_thenResponseIsNotFound() {
        // Given
        Long taskId = 1L;
        when(taskRepository.findById(taskId)).thenReturn(null);

        // When
        Response response = taskItemService.createTaskItems(taskId, List.of());

        // Then
        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());
    }

    @Test
    void givenTaskOwnedByDifferentUser_whenCreateTaskItemsIsCalled_thenResponseIsUnauthorized() {
        // Given
        Long taskId = 1L;
        Task task = new Task();
        task.setUserId("otherUser");
        when(taskRepository.findById(taskId)).thenReturn(task);

        // When
        Response response = taskItemService.createTaskItems(taskId, List.of());

        // Then
        assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), response.getStatus());
    }

    @Test
    void givenExistingTaskItem_whenDeleteTaskItemIsCalled_thenTaskItemIsDeletedAndResponseIsNoContent() {
        // Given
        Long taskId = 1L;
        Long taskItemId = 2L;
        when(taskItemRepository.deleteByTaskIdAndTaskItemIdAndUserId(taskId, taskItemId, "testUser")).thenReturn(1L);

        // When
        Response response = taskItemService.deleteTaskItem(taskId, taskItemId);

        // Then
        assertEquals(Response.Status.NO_CONTENT.getStatusCode(), response.getStatus());
    }

    @Test
    void givenNonExistentTaskItem_whenDeleteTaskItemIsCalled_thenResponseIsNotFound() {
        // Given
        Long taskId = 1L;
        Long taskItemId = 2L;
        when(taskItemRepository.deleteByTaskIdAndTaskItemIdAndUserId(taskId, taskItemId, "testUser")).thenReturn(0L);

        // When
        Response response = taskItemService.deleteTaskItem(taskId, taskItemId);

        // Then
        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());
    }
}
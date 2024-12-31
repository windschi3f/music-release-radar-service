package com.windschief.task;

import java.util.Arrays;
import java.util.List;

import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;

import com.windschief.task.item.TaskItem;
import com.windschief.task.item.TaskItemRequestDto;
import com.windschief.task.item.TaskItemResponseDto;
import com.windschief.task.item.TaskItemService;
import com.windschief.task.item.TaskItemType;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@QuarkusTest
class TaskItemServiceTest {
    private final TaskAccess taskAccess = mock(TaskAccess.class);
    private final TaskRepository taskRepository = mock(TaskRepository.class);
    private final TaskItemRepository taskItemRepository = mock(TaskItemRepository.class);
    private TaskItemService taskItemService;

    @BeforeEach
    public void setup() {
        taskItemService = new TaskItemService(taskAccess, taskRepository, taskItemRepository);
    }

    @Test
    @SuppressWarnings("unchecked")
    void givenExistingTaskItems_whenGetTaskItemsIsCalled_thenAllTaskItemsAreReturned() {
        // GIVEN
        Long taskId = 1L;
        Task task = new Task();
        TaskItem item1 = new TaskItem();
        TaskItem item2 = new TaskItem();

        when(taskRepository.findById(taskId)).thenReturn(task);
        when(taskItemRepository.findByTaskId(taskId)).thenReturn(Arrays.asList(item1, item2));

        // WHEN
        Response result = taskItemService.getTaskItems(taskId);

        // THEN
        assertEquals(Response.Status.OK.getStatusCode(), result.getStatus());
        List<TaskItemResponseDto> taskItems = (List<TaskItemResponseDto>) result.getEntity();
        assertEquals(2, taskItems.size());
        verify(taskItemRepository).findByTaskId(taskId);
    }

    @Test
    void givenValidTaskAndTaskItems_whenCreateTaskItemsIsCalled_thenTaskItemsAreCreatedAndResponseIsCreated() {
        // GIVEN
        Long taskId = 1L;
        Task task = new Task();
        task.setUserId("testUser");
        when(taskRepository.findById(taskId)).thenReturn(task);

        TaskItemRequestDto dto1 = new TaskItemRequestDto(TaskItemType.PLAYLIST, "1");
        TaskItemRequestDto dto2 = new TaskItemRequestDto(TaskItemType.PLAYLIST, "2");
        List<TaskItemRequestDto> dtos = Arrays.asList(dto1, dto2);

        // WHEN
        Response response = taskItemService.createTaskItems(taskId, dtos);

        // THEN
        assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());
        verify(taskItemRepository).persist(anyList());
    }

    @Test
    void givenTaskAccessException_whenCreateTaskItemsIsCalled_thenThrowException() {
        // GIVEN
        Long taskId = 1L;
        Task task = null;
        when(taskRepository.findById(taskId)).thenReturn(task);
        doThrow(new NotFoundException()).when(taskAccess).checkAccess(task);

        // WHEN / THEN
        assertThrows(NotFoundException.class, () -> taskItemService.createTaskItems(taskId, List.of()));
    }

    @Test
    void givenExistingTaskItem_whenDeleteTaskItemIsCalled_thenTaskItemIsDeletedAndResponseIsNoContent() {
        // GIVEN
        Long taskId = 1L;
        Long taskItemId = 2L;
        Task task = new Task();
        when(taskRepository.findById(taskId)).thenReturn(task);
        when(taskAccess.getCurrentUserId()).thenReturn("testUser");
        when(taskItemRepository.deleteByTaskIdAndTaskItemIdAndUserId(taskId, taskItemId, taskAccess.getCurrentUserId()))
                .thenReturn(1L);

        // WHEN
        Response response = taskItemService.deleteTaskItem(taskId, taskItemId);

        // THEN
        assertEquals(Response.Status.NO_CONTENT.getStatusCode(), response.getStatus());
    }

    @Test
    void givenNonExistentTaskItem_whenDeleteTaskItemIsCalled_thenResponseIsNotFound() {
        // GIVEN
        Long taskId = 1L;
        Long taskItemId = 2L;
        Task task = new Task();
        when(taskRepository.findById(taskId)).thenReturn(task);
        when(taskItemRepository.deleteByTaskIdAndTaskItemIdAndUserId(taskId, taskItemId, taskAccess.getCurrentUserId()))
                .thenReturn(0L);
        when(taskAccess.getCurrentUserId()).thenReturn("testUser");

        // WHEN
        Response response = taskItemService.deleteTaskItem(taskId, taskItemId);

        // THEN
        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());
    }
}
package com.windschief.task.added_item;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.windschief.task.Task;
import com.windschief.task.TaskAccess;
import com.windschief.task.TaskRepository;

import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;

class AddedItemServiceTest {
    private final TaskAccess taskAccess = mock(TaskAccess.class);
    private final TaskRepository taskRepository = mock(TaskRepository.class);
    private final AddedItemRepository addedItemRepository = mock(AddedItemRepository.class);
    private final AddedItemService addedItemService = new AddedItemService(taskAccess, taskRepository, addedItemRepository);

    @Test
    @SuppressWarnings("unchecked")
    void givenExistingTask_whenGetAddedItems_thenItemsAreReturned() {
        // GIVEN
        Long taskId = 1L;
        Task task = new Task();
        task.setUserId("testUser");
        AddedItem addedItem = new AddedItem();
        addedItem.setTask(task);
        addedItem.setExternalId("spotify:track:123");

        when(taskRepository.findById(taskId)).thenReturn(task);
        when(addedItemRepository.findByTaskId(taskId)).thenReturn(List.of(addedItem));

        // WHEN
        Response response = addedItemService.getAddedItems(taskId);

        // THEN
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        List<AddedItem> items = (List<AddedItem>) response.getEntity();
        assertEquals(1, items.size());
        assertEquals("spotify:track:123", items.get(0).getExternalId());
    }

    @Test
    void givenTaskAccessException_whenGetAddedItems_thenExceptionIsThrown() {
        // GIVEN
        Long taskId = 1L;
        doThrow(new NotFoundException()).when(taskAccess).checkAccess(taskId);

        // WHEN / THEN
        assertThrows(NotFoundException.class, () -> addedItemService.getAddedItems(taskId));
    }
}
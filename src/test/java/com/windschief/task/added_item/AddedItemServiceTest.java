package com.windschief.task.added_item;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.windschief.task.Task;
import com.windschief.task.TaskAccess;
import com.windschief.task.TaskRepository;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.ws.rs.core.Response;

@QuarkusTest
class AddedItemServiceTest {
    private final TaskAccess taskAccess = mock(TaskAccess.class);
    private final TaskRepository taskRepository = mock(TaskRepository.class);
    private final AddedItemRepository addedItemRepository = mock(AddedItemRepository.class);
    private AddedItemService addedItemService;

    @BeforeEach
    void setup() {
        addedItemService = new AddedItemService(taskAccess, taskRepository, addedItemRepository);
    }

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
        when(taskAccess.checkAccess(task)).thenReturn(Optional.empty());
        when(addedItemRepository.findByTaskId(taskId)).thenReturn(List.of(addedItem));

        // WHEN
        Response response = addedItemService.getAddedItems(taskId);

        // THEN
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        List<AddedItemResponseDto> items = (List<AddedItemResponseDto>) response.getEntity();
        assertEquals(1, items.size());
        assertEquals("spotify:track:123", items.get(0).externalId());
    }

    @Test
    void givenNonExistentTask_whenGetAddedItems_thenResponseIsNotFound() {
        // GIVEN
        Long taskId = 1L;
        Task task = null;
        when(taskRepository.findById(taskId)).thenReturn(task);
        when(taskAccess.checkAccess(task)).thenReturn(
                Optional.of(Response.status(Response.Status.NOT_FOUND).build()));

        // WHEN
        Response response = addedItemService.getAddedItems(taskId);

        // THEN
        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());
    }

    @Test
    void givenTaskOwnedByDifferentUser_whenGetAddedItems_thenResponseIsUnauthorized() {
        // GIVEN
        Long taskId = 1L;
        Task task = new Task();
        task.setUserId("otherUser");
        when(taskRepository.findById(taskId)).thenReturn(task);
        when(taskAccess.checkAccess(task)).thenReturn(
                Optional.of(Response.status(Response.Status.UNAUTHORIZED).build()));

        // WHEN
        Response response = addedItemService.getAddedItems(taskId);

        // THEN
        assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), response.getStatus());
    }
}
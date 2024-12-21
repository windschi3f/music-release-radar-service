package com.windschief.task.added_item;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.security.Principal;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.windschief.task.Task;
import com.windschief.task.TaskRepository;

import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.ws.rs.core.Response;

@QuarkusTest
class AddedItemServiceTest {

    private final TaskRepository taskRepository = mock(TaskRepository.class);
    private final AddedItemRepository addedItemRepository = mock(AddedItemRepository.class);
    private final SecurityIdentity securityIdentity = mock(SecurityIdentity.class);
    private final Principal principal = mock(Principal.class);

    private AddedItemService addedItemService;

    @BeforeEach
    void setup() {
        addedItemService = new AddedItemService(securityIdentity, taskRepository, addedItemRepository);
        when(securityIdentity.getPrincipal()).thenReturn(principal);
        when(principal.getName()).thenReturn("testUser");
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
    void givenNonExistentTask_whenGetAddedItems_thenResponseIsUnauthorized() {
        // GIVEN
        Long taskId = 1L;
        when(taskRepository.findById(taskId)).thenReturn(null);

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

        // WHEN
        Response response = addedItemService.getAddedItems(taskId);

        // THEN
        assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), response.getStatus());
    }
}
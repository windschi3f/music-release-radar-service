package com.windschief.user;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.security.Principal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.windschief.auth.SpotifyToken;
import com.windschief.auth.SpotifyTokenRepository;
import com.windschief.task.Task;
import com.windschief.task.TaskRepository;
import com.windschief.task.added_item.AddedItem;
import com.windschief.task.added_item.AddedItemRepository;
import com.windschief.task.item.TaskItem;
import com.windschief.task.item.TaskItemRepository;

import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.test.InjectMock;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;

@QuarkusTest
public class UserServiceTest {

    @Inject
    SpotifyTokenRepository spotifyTokenRepository;

    @Inject
    TaskRepository taskRepository;

    @Inject
    AddedItemRepository addedItemRepository;
    
    @Inject
    TaskItemRepository taskItemRepository;

    @Inject
    UserService userService;

    @InjectMock
    SecurityIdentity securityIdentity;
    
    @BeforeEach
    void setup() {
        Principal principal = mock(Principal.class);
        when(principal.getName()).thenReturn("test-user-id");
        when(securityIdentity.getPrincipal()).thenReturn(principal);
    }

    @Test
    @TestTransaction
    void givenUserData_whenDeleteUserData_thenDeleteUserData() {
        //GIVEN
        String userId = "test-user-id";
        
        SpotifyToken token = new SpotifyToken(userId, "access-token", "refresh-token", null);
        spotifyTokenRepository.persist(token);

        Task task = new Task();
        task.setUserId(userId);
        task.setName("Test Task");
        taskRepository.persist(task);

        TaskItem item = new TaskItem();
        item.setTask(task);
        item.setExternalId("external-id");
        taskItemRepository.persist(item);

        AddedItem addedItem = new AddedItem();
        addedItem.setTask(task);
        addedItem.setExternalId("external-id");
        addedItemRepository.persist(addedItem);

        //WHEN
        userService.deleteUserData();

        //THEN
        assert(spotifyTokenRepository.findByUserId(userId).isEmpty());
        assert(taskRepository.countTasksForUser(userId) == 0);
        assert(addedItemRepository.count("task.userId", userId) == 0);
        assert(taskItemRepository.count("task.userId", userId) == 0);
    }

    @Test
    @TestTransaction
    void givenUserData_whenGetUserData_thenReturnAllUserData() {
        //GIVEN
        String userId = "test-user-id";

        SpotifyToken token = new SpotifyToken(userId, "access-token", "refresh-token", null);
        spotifyTokenRepository.persist(token);

        Task task = new Task();
        task.setUserId(userId);
        task.setName("Test Task");
        taskRepository.persist(task);

        TaskItem item = new TaskItem();
        item.setTask(task);
        item.setExternalId("external-id");
        taskItemRepository.persist(item);

        AddedItem addedItem = new AddedItem();
        addedItem.setTask(task);
        addedItem.setExternalId("external-id");
        addedItemRepository.persist(addedItem);

        //WHEN
        Response response = userService.getUserData();

        //THEN
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        UserDataDto data = (UserDataDto) response.getEntity();
        assertNotNull(data);
        assertEquals(userId, data.userId());
        assertNotNull(data.spotifyToken());
        assertEquals("access-token", data.spotifyToken().getAccessToken());
        assertEquals(1, data.tasks().size());
        assertEquals("Test Task", data.tasks().get(0).getName());
        assertEquals(1, data.addedItems().size());
        assertEquals("external-id", data.addedItems().get(0).getExternalId());
    }
}

package com.windschief.user;

import com.windschief.auth.SpotifyTokenRepository;
import com.windschief.task.TaskRepository;
import com.windschief.task.added_item.AddedItemRepository;
import com.windschief.task.item.TaskItemRepository;

import io.quarkus.security.identity.SecurityIdentity;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.core.Response;

@ApplicationScoped
public class UserService implements UserApi {

    private final SecurityIdentity securityIdentity;
    private final SpotifyTokenRepository spotifyTokenRepository;
    private final TaskRepository taskRepository;
    private final AddedItemRepository addedItemRepository;
    private final TaskItemRepository taskItemRepository;

    @Inject
    public UserService(SecurityIdentity securityIdentity, SpotifyTokenRepository spotifyTokenRepository, TaskRepository taskRepository, AddedItemRepository addedItemRepository, TaskItemRepository taskItemRepository) {
        this.securityIdentity = securityIdentity;
        this.spotifyTokenRepository = spotifyTokenRepository;
        this.taskRepository = taskRepository;
        this.addedItemRepository = addedItemRepository;
        this.taskItemRepository = taskItemRepository;
    }

    @Override
    public Response getUserData() {
        String userId = securityIdentity.getPrincipal().getName();

        UserDataDto data = new UserDataDto(
            userId,
            spotifyTokenRepository.findByUserId(userId).orElse(null),
            taskRepository.findByUserId(userId),
            addedItemRepository.findByUserId(userId)
        );

        return Response.ok(data).build();
    }

    @Override
    @Transactional
    public Response deleteUserData() {
        String userId = securityIdentity.getPrincipal().getName();

        taskItemRepository.deleteByUserId(userId);
        addedItemRepository.deleteByUserId(userId);
        taskRepository.deleteByUserId(userId);
        spotifyTokenRepository.deleteByUserId(userId);

        return Response.noContent().build();
    }
}

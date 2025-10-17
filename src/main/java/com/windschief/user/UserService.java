package com.windschief.user;

import com.windschief.auth.SpotifyTokenRepository;
import com.windschief.task.TaskMapper;
import com.windschief.task.TaskRepository;
import com.windschief.task.added_item.AddedItemRepository;
import com.windschief.task.item.TaskItemRepository;
import com.windschief.task.item.TaskItemResponseDto;

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
    private final TaskMapper taskMapper;

    @Inject
    public UserService(SecurityIdentity securityIdentity, SpotifyTokenRepository spotifyTokenRepository, TaskRepository taskRepository, AddedItemRepository addedItemRepository, TaskItemRepository taskItemRepository, TaskMapper taskMapper) {
        this.securityIdentity = securityIdentity;
        this.spotifyTokenRepository = spotifyTokenRepository;
        this.taskRepository = taskRepository;
        this.addedItemRepository = addedItemRepository;
        this.taskItemRepository = taskItemRepository;
        this.taskMapper = taskMapper;
    }

    @Override
    public Response getUserData() {
        String userId = securityIdentity.getPrincipal().getName();

        UserDataDto data = new UserDataDto(
            userId,
            spotifyTokenRepository.findByUserId(userId).orElse(null),
            taskRepository.findByUserId(userId).stream()
                .map(taskMapper::toDto)
                .toList(),
            taskItemRepository.findByUserId(userId).stream()
                .map(item -> new TaskItemResponseDto(
                    item.getId(),
                    item.getItemType(),
                    item.getExternalId()))
                .toList(),
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

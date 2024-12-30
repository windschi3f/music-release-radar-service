package com.windschief.task;

import java.util.Optional;

import io.quarkus.security.identity.SecurityIdentity;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;

@RequestScoped
public class TaskAccess {
    private final SecurityIdentity securityIdentity;
    private final TaskRepository taskRepository;

    @Inject
    public TaskAccess(SecurityIdentity securityIdentity, TaskRepository taskRepository) {
        this.securityIdentity = securityIdentity;
        this.taskRepository = taskRepository;
    }

    public Optional<Response> checkAccess(Task task) {
        if (task == null) {
            return Optional.of(Response.status(Response.Status.NOT_FOUND).build());
        } else if (!task.getUserId().equals(securityIdentity.getPrincipal().getName())) {
            return Optional.of(Response.status(Response.Status.UNAUTHORIZED).build());
        } else {
            return Optional.empty();
        }
    }

    public Optional<Response> checkAccess(Long taskId) {
        return checkAccess(taskRepository.findById(taskId));
    }

    public String getCurrentUserId() {
        return securityIdentity.getPrincipal().getName();
    }

    public SecurityIdentity getSecurityIdentity() {
        return securityIdentity;
    }
}
package com.windschief.task;

import io.quarkus.security.identity.SecurityIdentity;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.NotFoundException;

@RequestScoped
public class TaskAccess {
    private final SecurityIdentity securityIdentity;
    private final TaskRepository taskRepository;

    @Inject
    public TaskAccess(SecurityIdentity securityIdentity, TaskRepository taskRepository) {
        this.securityIdentity = securityIdentity;
        this.taskRepository = taskRepository;
    }

    public void checkAccess(Task task) {
        if (task == null) {
            throw new NotFoundException("Task not found");
        }
        if (!task.getUserId().equals(securityIdentity.getPrincipal().getName())) {
            throw new NotAuthorizedException("You are not authorized to access this task");
        }
    }

    public void checkAccess(Long taskId) {
        checkAccess(taskRepository.findById(taskId));
    }

    public String getCurrentUserId() {
        return securityIdentity.getPrincipal().getName();
    }

    public SecurityIdentity getSecurityIdentity() {
        return securityIdentity;
    }
}
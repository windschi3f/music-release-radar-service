package com.windschief.task;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import com.windschief.task.item.TaskItem;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;

@Entity
public class Task {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToMany(mappedBy = "task", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TaskItem> taskItems = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    private Platform platform;
    private String name;
    private int executionIntervalDays = 7;
    private Instant lastTimeExecuted;
    private Instant checkFrom;
    private boolean active;
    private String userId;
    private String playlistId;

    public boolean isDue() {
        return lastTimeExecuted == null ||
                lastTimeExecuted.plus(executionIntervalDays, ChronoUnit.DAYS).isBefore(Instant.now());
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public List<TaskItem> getTaskItems() {
        return taskItems;
    }

    public void addTaskItem(TaskItem item) {
        taskItems.add(item);
        item.setTask(this);
    }

    public void removeTaskItem(TaskItem item) {
        taskItems.remove(item);
        item.setTask(null);
    }

    public Platform getPlatform() {
        return platform;
    }

    public void setPlatform(Platform platform) {
        this.platform = platform;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getExecutionIntervalDays() {
        return executionIntervalDays;
    }

    public void setExecutionIntervalDays(int executionIntervalDays) {
        this.executionIntervalDays = executionIntervalDays;
    }

    public Instant getLastTimeExecuted() {
        return lastTimeExecuted;
    }

    public void setLastTimeExecuted(Instant lastTimeExecuted) {
        this.lastTimeExecuted = lastTimeExecuted;
    }

    public Instant getCheckFrom() {
        return checkFrom;
    }

    public void setCheckFrom(Instant checkFrom) {
        this.checkFrom = checkFrom;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getPlaylistId() {
        return playlistId;
    }

    public void setPlaylistId(String playlistId) {
        this.playlistId = playlistId;
    }
}
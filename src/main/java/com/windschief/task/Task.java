package com.windschief.task;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import com.windschief.task.item.TaskItem;

import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
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
    private LocalDate checkFrom;
    private boolean active;
    private String userId;
    private String playlistId;
    private String favoritePlaylistId;
    @Enumerated(EnumType.STRING)
    private TasteSource tasteSource;
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "task_source_playlist", joinColumns = @JoinColumn(name = "task_id"))
    @Column(name = "playlist_id")
    private List<String> sourcePlaylistIds = new ArrayList<>();
    private int recentDays = 7;
    private int discoveryLimit = 20;
    private boolean replacePlaylist;
    @jakarta.persistence.Column(length = 1000)
    private String lastRunMessage;
    private boolean lastRunFailed;

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

    public LocalDate getCheckFrom() {
        return checkFrom;
    }

    public void setCheckFrom(LocalDate checkFrom) {
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

    public List<String> getSourcePlaylistIds() { return sourcePlaylistIds; }
    public void setSourcePlaylistIds(List<String> ids) {
        sourcePlaylistIds.clear();
        if (ids != null) sourcePlaylistIds.addAll(ids);
    }
    public int getRecentDays() { return recentDays; }
    public void setRecentDays(int value) { recentDays = value; }
    public int getDiscoveryLimit() { return discoveryLimit; }
    public void setDiscoveryLimit(int value) { discoveryLimit = value; }
    public boolean isReplacePlaylist() { return replacePlaylist; }
    public void setReplacePlaylist(boolean value) { replacePlaylist = value; }
    public String getLastRunMessage() { return lastRunMessage; }
    public void setLastRunMessage(String value) { lastRunMessage = value; }
    public boolean isLastRunFailed() { return lastRunFailed; }
    public void setLastRunFailed(boolean value) { lastRunFailed = value; }

    public TasteSource getTasteSource() {
        return tasteSource;
    }

    public void setTasteSource(TasteSource tasteSource) {
        this.tasteSource = tasteSource;
    }

    public String getFavoritePlaylistId() {
        return favoritePlaylistId;
    }

    public void setFavoritePlaylistId(String favoritePlaylistId) {
        this.favoritePlaylistId = favoritePlaylistId;
    }

    public String getPlaylistId() {
        return playlistId;
    }

    public void setPlaylistId(String playlistId) {
        this.playlistId = playlistId;
    }
}
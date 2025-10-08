package com.windschief.task.item;

import com.windschief.task.Task;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

@Entity
public class TaskItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "task_id")
    private Task task;

    @Enumerated(EnumType.STRING)
    private TaskItemType itemType;

    private String externalId;

    public Long getId() {
        return id;
    }

    public Task getTask() {
        return task;
    }

    public TaskItemType getItemType() {
        return itemType;
    }

    public String getExternalId() {
        return externalId;
    }

    public void setTask(Task task) {
        this.task = task;
    }

    public void setItemType(TaskItemType itemType) {
        this.itemType = itemType;
    }

    public void setExternalId(String externalId) {
        this.externalId = externalId;
    }
}
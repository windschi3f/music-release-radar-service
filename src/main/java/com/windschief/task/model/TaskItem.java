package com.windschief.task.model;

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

    private String itemId;

    public TaskItemType getItemType() {
        return itemType;
    }

    public String getItemId() {
        return itemId;
    }

    public void setTask(Task task) {
        this.task = task;
    }

    public void setItemType(TaskItemType itemType) {
        this.itemType = itemType;
    }

    public void setItemId(String itemId) {
        this.itemId = itemId;
    }
}
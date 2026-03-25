package com.tasktracker.task_tracker_api.entity;

import com.tasktracker.task_tracker_api.enums.TaskHistoryAction;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "task_history")
public class TaskHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long taskId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TaskHistoryAction action;

    private String oldValue;

    private String newValue;

    @Column(nullable = false)
    private String performedBy;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    public TaskHistory() {
    }

    public TaskHistory(Long id, Long taskId, TaskHistoryAction action, String oldValue, String newValue,
                       String performedBy, LocalDateTime timestamp) {
        this.id = id;
        this.taskId = taskId;
        this.action = action;
        this.oldValue = oldValue;
        this.newValue = newValue;
        this.performedBy = performedBy;
        this.timestamp = timestamp;
    }

    @PrePersist
    public void prePersist() {
        if (timestamp == null) {
            timestamp = LocalDateTime.now();
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getTaskId() {
        return taskId;
    }

    public void setTaskId(Long taskId) {
        this.taskId = taskId;
    }

    public TaskHistoryAction getAction() {
        return action;
    }

    public void setAction(TaskHistoryAction action) {
        this.action = action;
    }

    public String getOldValue() {
        return oldValue;
    }

    public void setOldValue(String oldValue) {
        this.oldValue = oldValue;
    }

    public String getNewValue() {
        return newValue;
    }

    public void setNewValue(String newValue) {
        this.newValue = newValue;
    }

    public String getPerformedBy() {
        return performedBy;
    }

    public void setPerformedBy(String performedBy) {
        this.performedBy = performedBy;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}

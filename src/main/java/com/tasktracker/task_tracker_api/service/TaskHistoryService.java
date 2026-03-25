package com.tasktracker.task_tracker_api.service;

import com.tasktracker.task_tracker_api.config.StringConstants;
import com.tasktracker.task_tracker_api.dto.TaskHistoryResponse;
import com.tasktracker.task_tracker_api.entity.TaskHistory;
import com.tasktracker.task_tracker_api.enums.TaskHistoryAction;
import com.tasktracker.task_tracker_api.repository.TaskHistoryRepository;
import com.tasktracker.task_tracker_api.repository.TaskRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class TaskHistoryService {

    private final TaskHistoryRepository taskHistoryRepository;
    private final TaskRepository taskRepository;

    public TaskHistoryService(TaskHistoryRepository taskHistoryRepository, TaskRepository taskRepository) {
        this.taskHistoryRepository = taskHistoryRepository;
        this.taskRepository = taskRepository;
    }

    public void saveHistory(Long taskId, TaskHistoryAction action, String oldValue, String newValue, String performedBy) {
        TaskHistory taskHistory = new TaskHistory();
        taskHistory.setTaskId(taskId);
        taskHistory.setAction(action);
        taskHistory.setOldValue(oldValue);
        taskHistory.setNewValue(newValue);
        taskHistory.setPerformedBy(performedBy);
        taskHistory.setTimestamp(LocalDateTime.now());
        taskHistoryRepository.save(taskHistory);
    }

    public List<TaskHistoryResponse> getTaskHistory(Long taskId) {
        taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException(StringConstants.ValidationMessages.TASK_NOT_FOUND));

        return taskHistoryRepository.findByTaskIdOrderByTimestampDesc(taskId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private TaskHistoryResponse mapToResponse(TaskHistory taskHistory) {
        return new TaskHistoryResponse(
                taskHistory.getId(),
                taskHistory.getTaskId(),
                taskHistory.getAction(),
                taskHistory.getOldValue(),
                taskHistory.getNewValue(),
                taskHistory.getPerformedBy(),
                taskHistory.getTimestamp()
        );
    }
}

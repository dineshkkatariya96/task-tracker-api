package com.tasktracker.task_tracker_api.service;

import com.tasktracker.task_tracker_api.config.StringConstants;
import com.tasktracker.task_tracker_api.dto.TaskHistoryResponse;
import com.tasktracker.task_tracker_api.entity.TaskHistory;
import com.tasktracker.task_tracker_api.enums.TaskHistoryAction;
import com.tasktracker.task_tracker_api.exception.ResourceNotFoundException;
import com.tasktracker.task_tracker_api.repository.TaskHistoryRepository;
import com.tasktracker.task_tracker_api.repository.TaskRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class TaskHistoryService {

    private static final Logger logger = LoggerFactory.getLogger(TaskHistoryService.class);

    private final TaskHistoryRepository taskHistoryRepository;
    private final TaskRepository taskRepository;

    public TaskHistoryService(TaskHistoryRepository taskHistoryRepository, TaskRepository taskRepository) {
        this.taskHistoryRepository = taskHistoryRepository;
        this.taskRepository = taskRepository;
    }

    public void saveHistory(Long taskId, TaskHistoryAction action, String oldValue, String newValue, String performedBy) {
        logger.debug("Starting saveHistory for taskId={} action={} performedBy={}",
                taskId, action, maskEmail(performedBy));
        TaskHistory taskHistory = new TaskHistory();
        taskHistory.setTaskId(taskId);
        taskHistory.setAction(action);
        taskHistory.setOldValue(oldValue);
        taskHistory.setNewValue(newValue);
        taskHistory.setPerformedBy(performedBy);
        taskHistory.setTimestamp(LocalDateTime.now());
        taskHistoryRepository.save(taskHistory);
        logger.debug("Completed saveHistory for taskId={} action={}", taskId, action);
    }

    public List<TaskHistoryResponse> getTaskHistory(Long taskId) {
        taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException(StringConstants.ValidationMessages.TASK_NOT_FOUND));

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

    private String maskEmail(String email) {
        if (email == null || email.isBlank()) {
            return "N/A";
        }

        int atIndex = email.indexOf('@');
        if (atIndex <= 1) {
            return "***";
        }

        return email.charAt(0) + "***" + email.substring(atIndex);
    }
}

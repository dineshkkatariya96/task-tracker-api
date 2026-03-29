package com.tasktracker.task_tracker_api.controller;

import com.tasktracker.task_tracker_api.config.StringConstants;
import com.tasktracker.task_tracker_api.dto.TaskHistoryResponse;
import com.tasktracker.task_tracker_api.dto.TaskRequest;
import com.tasktracker.task_tracker_api.dto.TaskResponse;
import com.tasktracker.task_tracker_api.enums.TaskStatus;
import com.tasktracker.task_tracker_api.service.TaskService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    private static final Logger logger = LoggerFactory.getLogger(TaskController.class);

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @PostMapping
    public ResponseEntity<TaskResponse> createTask(
            @Valid @RequestBody TaskRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        logger.info("POST /api/tasks hit by user={} assigneeId={} priority={} dueDate={}",
                maskEmail(userDetails.getUsername()), request.getAssigneeId(), request.getPriority(), request.getDueDate());
        return ResponseEntity.ok(
                taskService.createTask(request, userDetails.getUsername()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<TaskResponse> updateTask(
            @PathVariable Long id,
            @Valid @RequestBody TaskRequest request) {
        logger.info("PUT /api/tasks/{} hit with assigneeId={} priority={} dueDate={}",
                id, request.getAssigneeId(), request.getPriority(), request.getDueDate());
        return ResponseEntity.ok(taskService.updateTask(id, request));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<TaskResponse> updateStatus(
            @PathVariable Long id,
            @RequestParam TaskStatus status) {
        logger.info("PATCH /api/tasks/{}/status hit with status={}", id, status);
        return ResponseEntity.ok(taskService.updateStatus(id, status));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteTask(@PathVariable Long id) {
        logger.info("DELETE /api/tasks/{} hit", id);
        taskService.deleteTask(id);
        return ResponseEntity.ok(StringConstants.TaskMessages.TASK_DELETED_SUCCESSFULLY);
    }

    @GetMapping
    public ResponseEntity<List<TaskResponse>> getAllTasks() {
        return ResponseEntity.ok(taskService.getAllTasks());
    }

    @GetMapping("/my")
    public ResponseEntity<List<TaskResponse>> getMyTasks(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(
                taskService.getMyTasks(userDetails.getUsername()));
    }

    @GetMapping("/overdue")
    public ResponseEntity<List<TaskResponse>> getOverdueTasks() {
        return ResponseEntity.ok(taskService.getOverdueTasks());
    }

    @GetMapping("/overdue/count")
    public ResponseEntity<Long> getOverdueTaskCount() {
        return ResponseEntity.ok(taskService.getOverdueTaskCount());
    }

    @GetMapping("/{id}/history")
    public ResponseEntity<List<TaskHistoryResponse>> getTaskHistory(@PathVariable Long id) {
        return ResponseEntity.ok(taskService.getTaskHistory(id));
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

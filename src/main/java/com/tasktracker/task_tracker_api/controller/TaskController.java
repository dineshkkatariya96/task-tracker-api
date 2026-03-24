package com.tasktracker.task_tracker_api.controller;

import com.tasktracker.task_tracker_api.config.StringConstants;
import com.tasktracker.task_tracker_api.dto.TaskRequest;
import com.tasktracker.task_tracker_api.dto.TaskResponse;
import com.tasktracker.task_tracker_api.enums.TaskStatus;
import com.tasktracker.task_tracker_api.service.TaskService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @PostMapping
    public ResponseEntity<TaskResponse> createTask(
            @Valid @RequestBody TaskRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(
                taskService.createTask(request, userDetails.getUsername()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<TaskResponse> updateTask(
            @PathVariable Long id,
            @Valid @RequestBody TaskRequest request) {
        return ResponseEntity.ok(taskService.updateTask(id, request));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<TaskResponse> updateStatus(
            @PathVariable Long id,
            @RequestParam TaskStatus status) {
        return ResponseEntity.ok(taskService.updateStatus(id, status));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteTask(@PathVariable Long id) {
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
}

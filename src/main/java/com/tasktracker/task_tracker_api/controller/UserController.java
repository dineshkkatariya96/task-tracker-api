package com.tasktracker.task_tracker_api.controller;

import com.tasktracker.task_tracker_api.config.StringConstants;
import com.tasktracker.task_tracker_api.dto.UserResponse;
import com.tasktracker.task_tracker_api.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public ResponseEntity<List<UserResponse>> getAllEmployees() {
        return ResponseEntity.ok(userService.getAllEmployees());
    }

    @GetMapping("/employees")
    public ResponseEntity<List<UserResponse>> getEmployeesOnly() {
        return ResponseEntity.ok(userService.getEmployeesOnly());
    }

    @GetMapping("/test")
    public ResponseEntity<String> test() {
        return ResponseEntity.ok(StringConstants.HealthMessages.USER_CONTROLLER_WORKING);
    }
}
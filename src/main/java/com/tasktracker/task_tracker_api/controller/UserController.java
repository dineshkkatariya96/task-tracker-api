package com.tasktracker.task_tracker_api.controller;

import com.tasktracker.task_tracker_api.config.StringConstants;
import com.tasktracker.task_tracker_api.dto.UserResponse;
import com.tasktracker.task_tracker_api.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public ResponseEntity<List<UserResponse>> getAllEmployees() {
        logger.info("GET /api/users hit");
        return ResponseEntity.ok(userService.getAllEmployees());
    }

    @GetMapping("/employees")
    public ResponseEntity<List<UserResponse>> getEmployeesOnly() {
        logger.info("GET /api/users/employees hit");
        return ResponseEntity.ok(userService.getEmployeesOnly());
    }

    @GetMapping("/test")
    public ResponseEntity<String> test() {
        logger.info("GET /api/users/test hit");
        return ResponseEntity.ok(StringConstants.HealthMessages.USER_CONTROLLER_WORKING);
    }
}

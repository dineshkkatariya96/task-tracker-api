package com.tasktracker.task_tracker_api.controller;

import com.tasktracker.task_tracker_api.config.StringConstants;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HomeController {

    @GetMapping("/")
    public String home() {
        return StringConstants.HealthMessages.API_RUNNING;
    }
}

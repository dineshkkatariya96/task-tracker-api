package com.tasktracker.task_tracker_api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class TaskTrackerApiApplication {
	public static void main(String[] args) {
		SpringApplication.run(TaskTrackerApiApplication.class, args);
	}
}
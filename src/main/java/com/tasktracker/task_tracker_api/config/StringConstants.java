package com.tasktracker.task_tracker_api.config;

/**
 * Centralized utility class for all user-facing string messages.
 * Organizes messages by category for better maintainability.
 */
public class StringConstants {

    // Auth/Registration Messages
    public static class AuthMessages {
        public static final String EMAIL_ALREADY_EXISTS = "Email already exists!";
        public static final String USER_REGISTERED_SUCCESSFULLY = "User registered successfully";
        public static final String ADMIN_CREATED_SUCCESSFULLY = "Admin created successfully!";
        public static final String ONLY_ADMINS_CAN_CREATE = "Only admins can create admins!";
        public static final String INVALID_AUTHORIZATION_HEADER = "Invalid Authorization header";
    }

    // Task Operation Messages
    public static class TaskMessages {
        public static final String TASK_DELETED_SUCCESSFULLY = "Task deleted successfully";
        public static final String TASK_CREATED_SUCCESSFULLY = "Task created successfully";
        public static final String TASK_UPDATED_SUCCESSFULLY = "Task updated successfully";
    }

    // Validation/Error Messages
    public static class ValidationMessages {
        public static final String USER_NOT_FOUND = "User not found";
        public static final String ASSIGNEE_NOT_FOUND = "Assignee not found";
        public static final String TASK_NOT_FOUND = "Task not found";
        public static final String USER_NOT_FOUND_WITH_EMAIL = "User not found: ";
    }

    // Health Check Messages
    public static class HealthMessages {
        public static final String USER_CONTROLLER_WORKING = "UserController is working!";
        public static final String API_RUNNING = "Task Tracker API is running";
    }
}

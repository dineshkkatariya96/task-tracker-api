package com.tasktracker.task_tracker_api.service;

import com.tasktracker.task_tracker_api.dto.UserResponse;
import com.tasktracker.task_tracker_api.entity.User;
import com.tasktracker.task_tracker_api.enums.Role;
import com.tasktracker.task_tracker_api.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public List<UserResponse> getAllEmployees() {
        logger.info("Starting getAllEmployees");
        logger.debug("Fetching all users from database");
        List<UserResponse> userResponses = userRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
        logger.info("Completed getAllEmployees with resultCount={}", userResponses.size());
        return userResponses;
    }

    public List<UserResponse> getEmployeesOnly() {
        logger.info("Starting getEmployeesOnly");
        logger.debug("Fetching users for employee-only filter");
        List<UserResponse> userResponses = userRepository.findAll()
                .stream()
                .filter(u -> u.getRole() == Role.EMPLOYEE)
                .map(this::mapToResponse)
                .collect(Collectors.toList());
        logger.info("Completed getEmployeesOnly with resultCount={}", userResponses.size());
        return userResponses;
    }

    private UserResponse mapToResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole()
        );
    }
}

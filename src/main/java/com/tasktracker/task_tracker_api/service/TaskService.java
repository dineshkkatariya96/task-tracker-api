package com.tasktracker.task_tracker_api.service;

import com.tasktracker.task_tracker_api.config.StringConstants;
import com.tasktracker.task_tracker_api.dto.TaskHistoryResponse;
import com.tasktracker.task_tracker_api.dto.TaskRequest;
import com.tasktracker.task_tracker_api.dto.TaskResponse;
import com.tasktracker.task_tracker_api.entity.Task;
import com.tasktracker.task_tracker_api.entity.User;
import com.tasktracker.task_tracker_api.enums.TaskHistoryAction;
import com.tasktracker.task_tracker_api.enums.TaskStatus;
import com.tasktracker.task_tracker_api.repository.TaskRepository;
import com.tasktracker.task_tracker_api.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class TaskService {

    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final TaskHistoryService taskHistoryService;

    public TaskService(TaskRepository taskRepository, UserRepository userRepository, TaskHistoryService taskHistoryService) {
        this.taskRepository = taskRepository;
        this.userRepository = userRepository;
        this.taskHistoryService = taskHistoryService;
    }

    @Transactional
    public TaskResponse createTask(TaskRequest request, String createdByEmail) {
        User createdBy = userRepository.findByEmail(createdByEmail)
                .orElseThrow(() -> new RuntimeException(StringConstants.ValidationMessages.USER_NOT_FOUND));

        User assignee = null;
        if (request.getAssigneeId() != null) {
            assignee = userRepository.findById(request.getAssigneeId())
                    .orElseThrow(() -> new RuntimeException(StringConstants.ValidationMessages.ASSIGNEE_NOT_FOUND));
        }

        Task task = new Task();
        task.setTitle(request.getTitle());
        task.setDescription(request.getDescription());
        task.setPriority(request.getPriority());
        task.setDueDate(request.getDueDate());
        task.setStatus(TaskStatus.OPEN);
        task.setAssignee(assignee);
        task.setCreatedBy(createdBy);

        Task savedTask = taskRepository.save(task);
        taskHistoryService.saveHistory(
                savedTask.getId(),
                TaskHistoryAction.CREATED,
                null,
                savedTask.getStatus().name(),
                createdByEmail
        );

        if (assignee != null) {
            taskHistoryService.saveHistory(
                    savedTask.getId(),
                    TaskHistoryAction.ASSIGNED,
                    null,
                    getUserIdentifier(assignee),
                    createdByEmail
            );
        }

        return mapToResponse(savedTask);
    }

    @Transactional
    public TaskResponse updateTask(Long id, TaskRequest request) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new RuntimeException(StringConstants.ValidationMessages.TASK_NOT_FOUND));
        User previousAssignee = task.getAssignee();

        task.setTitle(request.getTitle());
        task.setDescription(request.getDescription());
        task.setPriority(request.getPriority());
        task.setDueDate(request.getDueDate());

        if (request.getAssigneeId() != null) {
            User assignee = userRepository.findById(request.getAssigneeId())
                    .orElseThrow(() -> new RuntimeException(StringConstants.ValidationMessages.ASSIGNEE_NOT_FOUND));
            task.setAssignee(assignee);
        }

        Task savedTask = taskRepository.save(task);
        savedTask = applyDerivedTaskState(savedTask, getCurrentUsername());

        if (request.getAssigneeId() != null && hasAssigneeChanged(previousAssignee, savedTask.getAssignee())) {
            taskHistoryService.saveHistory(
                    savedTask.getId(),
                    TaskHistoryAction.ASSIGNED,
                    getUserIdentifier(previousAssignee),
                    getUserIdentifier(savedTask.getAssignee()),
                    getCurrentUsername()
            );
        }

        return mapToResponse(savedTask);
    }

    @Transactional
    public TaskResponse updateStatus(Long id, TaskStatus newStatus) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new RuntimeException(StringConstants.ValidationMessages.TASK_NOT_FOUND));
        TaskStatus oldStatus = task.getStatus();
        task.setStatus(newStatus);

        if (newStatus == TaskStatus.DONE) {
            task.setOverdue(false);
        }

        Task savedTask = taskRepository.save(task);

        if (oldStatus != newStatus) {
            taskHistoryService.saveHistory(
                    savedTask.getId(),
                    TaskHistoryAction.STATUS_CHANGED,
                    oldStatus != null ? oldStatus.name() : null,
                    newStatus.name(),
                    getCurrentUsername()
            );
        }

        savedTask = applyDerivedTaskState(savedTask, getCurrentUsername());
        return mapToResponse(savedTask);
    }

    public void deleteTask(Long id) {
        taskRepository.deleteById(id);
    }

    @Transactional
    public List<TaskResponse> getAllTasks() {
        syncOverdueTasks();
        return taskRepository.findAll()
                .stream().map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public List<TaskResponse> getMyTasks(String email) {
        syncOverdueTasks();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException(StringConstants.ValidationMessages.USER_NOT_FOUND));
        return taskRepository.findByAssignee(user)
                .stream().map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public List<TaskResponse> getOverdueTasks() {
        syncOverdueTasks();
        return taskRepository.findByOverdueTrue()
                .stream().map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public long getOverdueTaskCount() {
        syncOverdueTasks();
        return taskRepository.countByOverdueTrue();
    }

    public List<TaskHistoryResponse> getTaskHistory(Long taskId) {
        return taskHistoryService.getTaskHistory(taskId);
    }

    private TaskResponse mapToResponse(Task task) {
        return new TaskResponse(
                task.getId(),
                task.getTitle(),
                task.getDescription(),
                task.getStatus(),
                task.getPriority(),
                task.getDueDate(),
                task.isOverdue(),
                task.getAssignee() != null ? task.getAssignee().getName() : null,
                task.getCreatedBy() != null ? task.getCreatedBy().getName() : null
        );
    }

    private boolean hasAssigneeChanged(User previousAssignee, User currentAssignee) {
        if (previousAssignee == null && currentAssignee == null) {
            return false;
        }

        if (previousAssignee == null || currentAssignee == null) {
            return true;
        }

        return !previousAssignee.getId().equals(currentAssignee.getId());
    }

    private String getUserIdentifier(User user) {
        return user != null ? user.getEmail() : null;
    }

    private String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getName())) {
            return "SYSTEM";
        }
        return authentication.getName();
    }

    private void syncOverdueTasks() {
        LocalDate today = LocalDate.now();
        List<Task> tasksToUpdate = new ArrayList<>();

        List<Task> overdueTasks = taskRepository.findByStatusNotAndDueDateBefore(TaskStatus.DONE, today);
        for (Task task : overdueTasks) {
            if (task.getStatus() != TaskStatus.OVERDUE || !task.isOverdue()) {
                TaskStatus previousStatus = task.getStatus();
                task.setOverdue(true);
                task.setStatus(TaskStatus.OVERDUE);
                tasksToUpdate.add(task);

                if (previousStatus != TaskStatus.OVERDUE) {
                    taskHistoryService.saveHistory(
                            task.getId(),
                            TaskHistoryAction.STATUS_CHANGED,
                            previousStatus != null ? previousStatus.name() : null,
                            TaskStatus.OVERDUE.name(),
                            "SYSTEM"
                    );
                }
            }
        }

        List<Task> completedOverdueTasks = taskRepository.findByOverdueTrue()
                .stream()
                .filter(task -> task.getStatus() == TaskStatus.DONE)
                .toList();

        for (Task task : completedOverdueTasks) {
            task.setOverdue(false);
            tasksToUpdate.add(task);
        }

        if (!tasksToUpdate.isEmpty()) {
            taskRepository.saveAll(tasksToUpdate);
        }
    }

    private Task applyDerivedTaskState(Task task, String performedBy) {
        boolean updated = false;

        if (task.getStatus() == TaskStatus.DONE) {
            if (task.isOverdue()) {
                task.setOverdue(false);
                updated = true;
            }
        } else if (task.getDueDate() != null && task.getDueDate().isBefore(LocalDate.now())) {
            TaskStatus previousStatus = task.getStatus();

            if (task.getStatus() != TaskStatus.OVERDUE) {
                task.setStatus(TaskStatus.OVERDUE);
                updated = true;
                taskHistoryService.saveHistory(
                        task.getId(),
                        TaskHistoryAction.STATUS_CHANGED,
                        previousStatus != null ? previousStatus.name() : null,
                        TaskStatus.OVERDUE.name(),
                        performedBy
                );
            }

            if (!task.isOverdue()) {
                task.setOverdue(true);
                updated = true;
            }
        } else if (task.getStatus() != TaskStatus.OVERDUE && task.isOverdue()) {
            task.setOverdue(false);
            updated = true;
        }

        return updated ? taskRepository.save(task) : task;
    }
}

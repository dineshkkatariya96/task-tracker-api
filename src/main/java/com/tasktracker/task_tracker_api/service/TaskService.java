package com.tasktracker.task_tracker_api.service;

import com.tasktracker.task_tracker_api.config.StringConstants;
import com.tasktracker.task_tracker_api.dto.TaskHistoryResponse;
import com.tasktracker.task_tracker_api.dto.TaskRequest;
import com.tasktracker.task_tracker_api.dto.TaskResponse;
import com.tasktracker.task_tracker_api.entity.Task;
import com.tasktracker.task_tracker_api.entity.User;
import com.tasktracker.task_tracker_api.enums.TaskHistoryAction;
import com.tasktracker.task_tracker_api.enums.TaskStatus;
import com.tasktracker.task_tracker_api.exception.ResourceNotFoundException;
import com.tasktracker.task_tracker_api.repository.TaskRepository;
import com.tasktracker.task_tracker_api.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger logger = LoggerFactory.getLogger(TaskService.class);

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
        logger.info("Starting createTask for creator={} assigneeId={} priority={} dueDate={}",
                maskEmail(createdByEmail), request.getAssigneeId(), request.getPriority(), request.getDueDate());
        logger.debug("Fetching creator user record for email={}", maskEmail(createdByEmail));
        User createdBy = userRepository.findByEmail(createdByEmail)
                .orElseThrow(() -> new ResourceNotFoundException(StringConstants.ValidationMessages.USER_NOT_FOUND));

        User assignee = null;
        if (request.getAssigneeId() != null) {
            logger.debug("Fetching assignee user record for assigneeId={}", request.getAssigneeId());
            assignee = userRepository.findById(request.getAssigneeId())
                    .orElseThrow(() -> new ResourceNotFoundException(StringConstants.ValidationMessages.ASSIGNEE_NOT_FOUND));
        }

        Task task = new Task();
        task.setTitle(request.getTitle());
        task.setDescription(request.getDescription());
        task.setPriority(request.getPriority());
        task.setDueDate(request.getDueDate());
        task.setStatus(TaskStatus.OPEN);
        task.setAssignee(assignee);
        task.setCreatedBy(createdBy);

        logger.debug("Persisting task entity for creator={}", maskEmail(createdByEmail));
        Task savedTask = taskRepository.save(task);
        logger.debug("Saving task creation history for taskId={}", savedTask.getId());
        taskHistoryService.saveHistory(
                savedTask.getId(),
                TaskHistoryAction.CREATED,
                null,
                savedTask.getStatus().name(),
                createdByEmail
        );

        if (assignee != null) {
            logger.debug("Saving task assignment history for taskId={} assigneeId={}", savedTask.getId(), assignee.getId());
            taskHistoryService.saveHistory(
                    savedTask.getId(),
                    TaskHistoryAction.ASSIGNED,
                    null,
                    getUserIdentifier(assignee),
                    createdByEmail
            );
        }

        logger.info("Completed createTask for taskId={}", savedTask.getId());
        return mapToResponse(savedTask);
    }

    @Transactional
    public TaskResponse updateTask(Long id, TaskRequest request) {
        logger.info("Starting updateTask for taskId={} assigneeId={} priority={} dueDate={}",
                id, request.getAssigneeId(), request.getPriority(), request.getDueDate());
        logger.debug("Fetching task for update taskId={}", id);
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(StringConstants.ValidationMessages.TASK_NOT_FOUND));
        User previousAssignee = task.getAssignee();

        task.setTitle(request.getTitle());
        task.setDescription(request.getDescription());
        task.setPriority(request.getPriority());
        task.setDueDate(request.getDueDate());

        if (request.getAssigneeId() != null) {
            logger.debug("Fetching assignee for task update taskId={} assigneeId={}", id, request.getAssigneeId());
            User assignee = userRepository.findById(request.getAssigneeId())
                    .orElseThrow(() -> new ResourceNotFoundException(StringConstants.ValidationMessages.ASSIGNEE_NOT_FOUND));
            task.setAssignee(assignee);
        }

        logger.debug("Persisting updated task entity for taskId={}", id);
        Task savedTask = taskRepository.save(task);
        savedTask = applyDerivedTaskState(savedTask, getCurrentUsername());

        if (request.getAssigneeId() != null && hasAssigneeChanged(previousAssignee, savedTask.getAssignee())) {
            logger.debug("Saving assignment change history for taskId={}", savedTask.getId());
            taskHistoryService.saveHistory(
                    savedTask.getId(),
                    TaskHistoryAction.ASSIGNED,
                    getUserIdentifier(previousAssignee),
                    getUserIdentifier(savedTask.getAssignee()),
                    getCurrentUsername()
            );
        }

        logger.info("Completed updateTask for taskId={}", savedTask.getId());
        return mapToResponse(savedTask);
    }

    @Transactional
    public TaskResponse updateStatus(Long id, TaskStatus newStatus) {
        logger.info("Starting updateStatus for taskId={} newStatus={}", id, newStatus);
        logger.debug("Fetching task for status update taskId={}", id);
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(StringConstants.ValidationMessages.TASK_NOT_FOUND));
        TaskStatus oldStatus = task.getStatus();
        task.setStatus(newStatus);

        if (newStatus == TaskStatus.DONE) {
            task.setOverdue(false);
        }

        logger.debug("Persisting task status update for taskId={} oldStatus={} newStatus={}", id, oldStatus, newStatus);
        Task savedTask = taskRepository.save(task);

        if (oldStatus != newStatus) {
            logger.debug("Saving status change history for taskId={}", savedTask.getId());
            taskHistoryService.saveHistory(
                    savedTask.getId(),
                    TaskHistoryAction.STATUS_CHANGED,
                    oldStatus != null ? oldStatus.name() : null,
                    newStatus.name(),
                    getCurrentUsername()
            );
        }

        savedTask = applyDerivedTaskState(savedTask, getCurrentUsername());
        logger.info("Completed updateStatus for taskId={} finalStatus={}", savedTask.getId(), savedTask.getStatus());
        return mapToResponse(savedTask);
    }

    public void deleteTask(Long id) {
        logger.info("Starting deleteTask for taskId={}", id);
        if (!taskRepository.existsById(id)) {
            throw new ResourceNotFoundException(StringConstants.ValidationMessages.TASK_NOT_FOUND);
        }
        logger.debug("Deleting task record for taskId={}", id);
        taskRepository.deleteById(id);
        logger.info("Completed deleteTask for taskId={}", id);
    }

    @Transactional
    public List<TaskResponse> getAllTasks() {
        syncOverdueTasks();
        List<TaskResponse> taskResponses = taskRepository.findAll()
                .stream().map(this::mapToResponse)
                .collect(Collectors.toList());
        return taskResponses;
    }

    @Transactional
    public List<TaskResponse> getMyTasks(String email) {
        syncOverdueTasks();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException(StringConstants.ValidationMessages.USER_NOT_FOUND));
        List<TaskResponse> taskResponses = taskRepository.findByAssignee(user)
                .stream().map(this::mapToResponse)
                .collect(Collectors.toList());
        return taskResponses;
    }

    @Transactional
    public List<TaskResponse> getOverdueTasks() {
        syncOverdueTasks();
        List<TaskResponse> taskResponses = taskRepository.findByOverdueTrue()
                .stream().map(this::mapToResponse)
                .collect(Collectors.toList());
        return taskResponses;
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

        logger.debug("Syncing overdue tasks for date={}", today);
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
            logger.debug("Persisting overdue sync updates for taskCount={}", tasksToUpdate.size());
            taskRepository.saveAll(tasksToUpdate);
        }
    }

    private Task applyDerivedTaskState(Task task, String performedBy) {
        boolean updated = false;

        if (task.getStatus() == TaskStatus.DONE) {
            if (task.isOverdue()) {
                logger.debug("Clearing overdue flag for completed taskId={}", task.getId());
                task.setOverdue(false);
                updated = true;
            }
        } else if (task.getDueDate() != null && task.getDueDate().isBefore(LocalDate.now())) {
            TaskStatus previousStatus = task.getStatus();

            if (task.getStatus() != TaskStatus.OVERDUE) {
                logger.debug("Marking taskId={} as overdue", task.getId());
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
                logger.debug("Setting overdue flag for taskId={}", task.getId());
                task.setOverdue(true);
                updated = true;
            }
        } else if (task.getStatus() != TaskStatus.OVERDUE && task.isOverdue()) {
            logger.debug("Resetting overdue flag for taskId={}", task.getId());
            task.setOverdue(false);
            updated = true;
        }

        if (updated) {
            logger.debug("Persisting derived state changes for taskId={} performedBy={}", task.getId(), maskEmail(performedBy));
        }
        return updated ? taskRepository.save(task) : task;
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

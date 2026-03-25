package com.tasktracker.task_tracker_api.scheduler;

import com.tasktracker.task_tracker_api.entity.Task;
import com.tasktracker.task_tracker_api.enums.TaskHistoryAction;
import com.tasktracker.task_tracker_api.enums.TaskStatus;
import com.tasktracker.task_tracker_api.repository.TaskRepository;
import com.tasktracker.task_tracker_api.service.TaskHistoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Component
public class TaskOverdueScheduler {

    private static final Logger log = LoggerFactory.getLogger(TaskOverdueScheduler.class);

    private final TaskRepository taskRepository;
    private final TaskHistoryService taskHistoryService;

    public TaskOverdueScheduler(TaskRepository taskRepository, TaskHistoryService taskHistoryService) {
        this.taskRepository = taskRepository;
        this.taskHistoryService = taskHistoryService;
    }

    @Scheduled(cron = "0 0 0 * * *")
    @Transactional
    public void markOverdueTasks() {
        List<Task> overdueTasks = taskRepository
                .findByStatusNotAndDueDateBefore(
                        TaskStatus.DONE, LocalDate.now());

        overdueTasks.forEach(task -> {
            TaskStatus previousStatus = task.getStatus();
            task.setOverdue(true);
            task.setStatus(TaskStatus.OVERDUE);

            if (previousStatus != TaskStatus.OVERDUE) {
                taskHistoryService.saveHistory(
                        task.getId(),
                        TaskHistoryAction.STATUS_CHANGED,
                        previousStatus != null ? previousStatus.name() : null,
                        TaskStatus.OVERDUE.name(),
                        "SYSTEM"
                );
            }
        });

        taskRepository.saveAll(overdueTasks);
        log.info("Marked {} tasks as overdue", overdueTasks.size());
    }
}

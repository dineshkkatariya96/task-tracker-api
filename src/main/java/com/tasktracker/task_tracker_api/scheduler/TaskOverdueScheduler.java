package com.tasktracker.task_tracker_api.scheduler;

import com.tasktracker.task_tracker_api.entity.Task;
import com.tasktracker.task_tracker_api.enums.TaskStatus;
import com.tasktracker.task_tracker_api.repository.TaskRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
public class TaskOverdueScheduler {

    private static final Logger log = LoggerFactory.getLogger(TaskOverdueScheduler.class);

    private final TaskRepository taskRepository;

    public TaskOverdueScheduler(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    @Scheduled(cron = "0 0 0 * * *")
    public void markOverdueTasks() {
        List<Task> overdueTasks = taskRepository
                .findByStatusNotAndDueDateBefore(
                        TaskStatus.DONE, LocalDate.now());

        overdueTasks.forEach(task -> {
            task.setOverdue(true);
            task.setStatus(TaskStatus.OVERDUE);
        });

        taskRepository.saveAll(overdueTasks);
        log.info("Marked {} tasks as overdue", overdueTasks.size());
    }
}

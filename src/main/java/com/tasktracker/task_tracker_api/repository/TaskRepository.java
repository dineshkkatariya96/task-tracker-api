package com.tasktracker.task_tracker_api.repository;

import com.tasktracker.task_tracker_api.entity.Task;
import com.tasktracker.task_tracker_api.entity.User;
import com.tasktracker.task_tracker_api.enums.TaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {

    List<Task> findByAssignee(User assignee);

    List<Task> findByStatusNotAndDueDateBefore(TaskStatus status, LocalDate date);

    List<Task> findByOverdueTrue();
}

package com.tasktracker.task_tracker_api.repository;

import com.tasktracker.task_tracker_api.entity.TaskHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TaskHistoryRepository extends JpaRepository<TaskHistory, Long> {

    List<TaskHistory> findByTaskIdOrderByTimestampDesc(Long taskId);
}

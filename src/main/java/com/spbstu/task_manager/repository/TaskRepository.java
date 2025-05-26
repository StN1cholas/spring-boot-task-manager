package com.spbstu.task_manager.repository;

import com.spbstu.task_manager.model.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {

    // Найти все задачи для пользователя, которые не помечены как удаленные
    List<Task> findByUserIdAndDeletedFalse(Long userId);

    // Найти все задачи для пользователя, которые не помечены как удаленные и
    // targetDate которых позже указанной даты
    List<Task> findByUserIdAndDeletedFalseAndTargetDateAfter(Long userId, LocalDate date);

    // Можно добавить и другие методы, если понадобятся, например:
    // List<Task> findByUserIdAndDeletedFalseAndTargetDateBefore(Long userId, LocalDate date); // для просроченных
}
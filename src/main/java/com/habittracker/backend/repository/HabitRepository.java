package com.habittracker.backend.repository;

import com.habittracker.backend.model.Habit;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalTime;
import java.util.List;

public interface HabitRepository extends JpaRepository<Habit, Long> {
    List<Habit> findByUserIdAndIsActiveTrue(Long userId);
    List<Habit> findByReminderTimeAndIsActiveTrue(LocalTime reminderTime);
}
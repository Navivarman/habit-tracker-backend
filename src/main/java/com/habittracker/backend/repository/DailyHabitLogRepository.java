package com.habittracker.backend.repository;

import com.habittracker.backend.model.DailyHabitLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface DailyHabitLogRepository extends JpaRepository<DailyHabitLog, Long> {

    Optional<DailyHabitLog> findByUserIdAndHabitIdAndLogDate(Long userId, Long habitId, LocalDate logDate);

    List<DailyHabitLog> findByUserIdAndLogDate(Long userId, LocalDate logDate);

    // Count total active habits scheduled for a user
    @Query("SELECT COUNT(h) FROM Habit h WHERE h.user.id = :userId AND h.isActive = true")
    long countTotalHabitsForUser(@Param("userId") Long userId);

    // Count how many habits were successfully completed today
    @Query("SELECT COUNT(d) FROM DailyHabitLog d WHERE d.user.id = :userId AND d.logDate = :logDate AND d.isCompleted = true")
    long countCompletedHabitsForUserAndDate(@Param("userId") Long userId, @Param("logDate") LocalDate logDate);

    List<DailyHabitLog> findByUserIdAndLogDateBetween(Long userId, LocalDate startDate, LocalDate endDate);
}
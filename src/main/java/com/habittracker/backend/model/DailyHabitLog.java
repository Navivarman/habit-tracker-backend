package com.habittracker.backend.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "daily_habit_logs",
        uniqueConstraints = {@UniqueConstraint(columnNames = {"user_id", "habit_id", "log_date"})})
@Data
@NoArgsConstructor
public class DailyHabitLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne
    @JoinColumn(name = "habit_id", nullable = false)
    private Habit habit;

    @Column(nullable = false)
    private LocalDate logDate;

    private boolean isCompleted = false;
    private LocalDateTime completedAt;
}
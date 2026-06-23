package com.habittracker.backend.service;

import com.habittracker.backend.model.DailyHabitLog;
import com.habittracker.backend.model.Habit;
import com.habittracker.backend.model.User;
import com.habittracker.backend.repository.DailyHabitLogRepository;
import com.habittracker.backend.repository.HabitRepository;
import com.habittracker.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
public class HabitService {

    @Autowired private HabitRepository habitRepository;
    @Autowired private DailyHabitLogRepository logRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private RoomService roomService;

    // 1. Create a brand new habit and seed today's log for it
    @Transactional
    public Habit createHabit(Long userId, String title, LocalTime reminderTime) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Habit habit = new Habit();
        habit.setUser(user);
        habit.setTitle(title);
        habit.setReminderTime(reminderTime);
        Habit savedHabit = habitRepository.save(habit);

        // Seed an empty log for today so it's ready to be checked off
        DailyHabitLog log = new DailyHabitLog();
        log.setUser(user);
        log.setHabit(savedHabit);
        log.setLogDate(LocalDate.now());
        logRepository.save(log);

        return savedHabit;
    }

    // 2. Fetch all current habits for a user
    public List<Habit> getActiveHabits(Long userId) {
        return habitRepository.findByUserIdAndIsActiveTrue(userId);
    }

    // 3. Toggle a habit's status for today (Complete / Incomplete)
    @Transactional
    public DailyHabitLog toggleHabitCompletion(Long userId, Long habitId, LocalDate date) {
        DailyHabitLog log = logRepository.findByUserIdAndHabitIdAndLogDate(userId, habitId, date)
                .orElseGet(() -> {
                    // Fallback if log doesn't exist yet for this date
                    User user = userRepository.findById(userId).orElseThrow();
                    Habit habit = habitRepository.findById(habitId).orElseThrow();
                    DailyHabitLog newLog = new DailyHabitLog();
                    newLog.setUser(user);
                    newLog.setHabit(habit);
                    newLog.setLogDate(date);
                    return newLog;
                });

        // Toggle state
        boolean currentStatus = log.isCompleted();
        log.setCompleted(!currentStatus);
        log.setCompletedAt(!currentStatus ? LocalDateTime.now() : null);

        DailyHabitLog savedLog = logRepository.save(log);
        roomService.evaluateLudoProgression(userId, date);
        return savedLog;
    }
}
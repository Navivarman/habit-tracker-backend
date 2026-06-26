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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
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
                    User user = userRepository.findById(userId).orElseThrow();
                    Habit habit = habitRepository.findById(habitId).orElseThrow();
                    DailyHabitLog newLog = new DailyHabitLog();
                    newLog.setUser(user);
                    newLog.setHabit(habit);
                    newLog.setLogDate(date);
                    return newLog;
                });

        // Capture the state BEFORE the toggle
        boolean wasCompletedBeforeClick = log.isCompleted();

        // Toggle state
        log.setCompleted(!wasCompletedBeforeClick);
        log.setCompletedAt(!wasCompletedBeforeClick ? LocalDateTime.now() : null);
        DailyHabitLog savedLog = logRepository.save(log);

        // If it WAS completed, but now it's not -> this is an UNDO action!
        boolean isUndo = wasCompletedBeforeClick;

        // Run the updated bidirectional engine
        roomService.evaluateLudoProgression(userId, date, isUndo);

        return savedLog;
    }

    @Transactional
    public Habit updateHabit(Long habitId, String newTitle, LocalTime newReminderTime) {
        Habit habit = habitRepository.findById(habitId).orElseThrow();
        habit.setTitle(newTitle);
        habit.setReminderTime(newReminderTime);
        return habitRepository.save(habit);
    }

    @Transactional
    public void deleteHabit(Long habitId) {
        Habit habit = habitRepository.findById(habitId).orElseThrow();
        // Soft delete to preserve historical data logging integrity
        habit.setActive(false);
        habitRepository.save(habit);
    }

    public List<Map<String, Object>> getHabitsWithStatusForDate(Long userId, LocalDate date) {
        // Fetch all active base habits
        List<Habit> activeHabits = habitRepository.findByUserIdAndIsActiveTrue(userId);
        List<Map<String, Object>> result = new ArrayList<>();

        for (Habit habit : activeHabits) {
            Map<String, Object> habitMap = new HashMap<>();

            // Removed the invalid utilizeDetails line completely 🛠️
            habitMap.put("id", habit.getId());
            habitMap.put("title", habit.getTitle());
            habitMap.put("reminderTime", habit.getReminderTime().toString());

            // Check if a completion log exists in the database for this specific day
            boolean isCompletedToday = logRepository.findByUserIdAndHabitIdAndLogDate(userId, habit.getId(), date)
                    .map(DailyHabitLog::isCompleted)
                    .orElse(false);

            habitMap.put("completed", isCompletedToday);
            result.add(habitMap);
        }

        return result;
    }
}
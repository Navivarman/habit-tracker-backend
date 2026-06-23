package com.habittracker.backend.service;

import com.habittracker.backend.model.DailyHabitLog;
import com.habittracker.backend.model.Habit;
import com.habittracker.backend.repository.DailyHabitLogRepository;
import com.habittracker.backend.repository.HabitRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Component
public class DailyResetScheduler {

    @Autowired private HabitRepository habitRepository;
    @Autowired private DailyHabitLogRepository logRepository;

    // This cron expression executes exactly at 00:00:00 AM every single night
    @Scheduled(cron = "0 0 0 * * *")
    @Transactional
    public void seedDailyLogsForNewDay() {
        LocalDate today = LocalDate.now();
        System.out.println("⏰ Midnight Clock Triggered! Seeding habit logs for: " + today);

        // 1. Fetch absolutely all active habits across the entire system
        List<Habit> activeHabits = habitRepository.findAllActiveHabits();

        int seededCount = 0;
        for (Habit habit : activeHabits) {
            // 2. Double-check to prevent duplicate row creation if the server restarted
            boolean exists = logRepository.findByUserIdAndHabitIdAndLogDate(
                    habit.getUser().getId(), habit.getId(), today
            ).isPresent();

            if (!exists) {
                DailyHabitLog blankLog = new DailyHabitLog();
                blankLog.setUser(habit.getUser());
                blankLog.setHabit(habit);
                blankLog.setLogDate(today);
                blankLog.setCompleted(false); // Starts uncompleted

                logRepository.save(blankLog);
                seededCount++;
            }
        }

        System.out.println("✅ Successfully initialized " + seededCount + " habit tracking records for today.");
    }
}
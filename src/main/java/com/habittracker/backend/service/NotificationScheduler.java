package com.habittracker.backend.service;

import com.habittracker.backend.model.Habit;
import com.habittracker.backend.repository.HabitRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalTime;
import java.util.List;

@Component
public class NotificationScheduler {

    @Autowired private HabitRepository habitRepository;

    // This cron expression runs exactly at second 0 of every single minute
    @Scheduled(cron = "0 * * * * *")
    public void sendPreReminders() {
        // Calculate the target time exactly 5 minutes into the future, dropping seconds/nanos
        LocalTime targetTime = LocalTime.now().plusMinutes(5).withSecond(0).withNano(0);

        // Custom repository query we will define next
        List<Habit> habitsToRemind = habitRepository.findByReminderTimeAndIsActiveTrue(targetTime);

        for (Habit habit : habitsToRemind) {
            // Right now, we will mock-log this to the console.
            // In a production app, you would wire up Firebase Cloud Messaging or an SMS/Email service here.
            System.out.println("🔔 NOTIFICATION SENT: Hey " + habit.getUser().getUsername() +
                    ", your habit '" + habit.getTitle() + "' starts at " + habit.getReminderTime() + " (In 5 minutes!)");
        }
    }
}
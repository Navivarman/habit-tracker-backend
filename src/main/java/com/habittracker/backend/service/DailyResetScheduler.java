package com.habittracker.backend.service;

import com.habittracker.backend.model.*;
import com.habittracker.backend.repository.*;
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
    @Autowired private RoomRepository roomRepository;
    @Autowired private RoomMemberRepository roomMemberRepository;
    @Autowired private BadgeRepository badgeRepository;
    @Autowired private UserRepository userRepository;

    // 1. DAILY NIGHTLY SEEDER
    // This cron expression executes exactly at 00:00:00 AM every single night
    @Scheduled(cron = "0 0 0 * * *")
    @Transactional
    public void seedDailyLogsForNewDay() {
        LocalDate today = LocalDate.now();
        System.out.println("⏰ Midnight Clock Triggered! Seeding habit logs for: " + today);

        // Fetch absolutely all active habits across the entire system
        List<Habit> activeHabits = habitRepository.findAllActiveHabits();

        int seededCount = 0;
        for (Habit habit : activeHabits) {
            // Double-check to prevent duplicate row creation if the server restarted
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

    // 2. 🏆 MONTHLY REWARDS & BADGE ENGINE
    // This cron fires at 11:59:59 PM on the last day of every month ("L" stands for Last day)
    @Scheduled(cron = "59 59 23 L * *")
    @Transactional
    public void processMonthlyRewards() {
        LocalDate today = LocalDate.now();
        System.out.println("🏆 End of Month Reached! Running Badge Evaluation Engine for " + today.getMonth());

        // 🥇 Evaluate Monthly Room Winners
        List<Room> activeRooms = roomRepository.findAll();
        for (Room room : activeRooms) {
            List<RoomMember> standings = roomMemberRepository.findByRoomIdOrderByCurrentStepDesc(room.getId());
            if (!standings.isEmpty()) {
                int highestStep = standings.get(0).getCurrentStep();

                // Award badges to whoever is tied for the farthest step at month-end
                for (RoomMember member : standings) {
                    if (member.getCurrentStep() == highestStep && highestStep > 0) {
                        Badge winBadge = new Badge();
                        winBadge.setUser(member.getUser());
                        winBadge.setBadgeType("MONTHLY_ROOM_WINNER");
                        winBadge.setDetails("Reached step " + highestStep + " in room: " + room.getName());
                        winBadge.setAwardedForMonth(today);
                        badgeRepository.save(winBadge);
                    }
                }
            }
        }

        // 🟢 Evaluate Perfect Consistency Badges
        List<User> allUsers = userRepository.findAll();
        LocalDate startOfMonth = today.withDayOfMonth(1);

        for (User user : allUsers) {
            List<DailyHabitLog> monthLogs = logRepository.findByUserIdAndLogDateBetween(user.getId(), startOfMonth, today);

            // Check if there are any habits assigned, and if ANY logs were left incomplete during the month
            long totalAssigned = monthLogs.size();
            long totalMissed = monthLogs.stream().filter(log -> !log.isCompleted()).count();

            if (totalAssigned > 0 && totalMissed == 0) {
                Badge perfectBadge = new Badge();
                perfectBadge.setUser(user);
                perfectBadge.setBadgeType("PERFECT_CONSISTENCY");
                perfectBadge.setDetails("Completed 100% of habits every day for the month of " + today.getMonth());
                perfectBadge.setAwardedForMonth(today);
                badgeRepository.save(perfectBadge);
                System.out.println("🎖️ Perfect Consistency Badge awarded to user: " + user.getUsername());
            }
        }
        System.out.println("✅ Badge distribution completed successfully.");
    }
}
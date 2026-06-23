package com.habittracker.backend.controller;

import com.habittracker.backend.model.DailyHabitLog;
import com.habittracker.backend.model.Habit;
import com.habittracker.backend.model.User;
import com.habittracker.backend.repository.UserRepository;
import com.habittracker.backend.service.HabitService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/habits")
@CrossOrigin(origins = "*") // Allows your React frontend to talk to this backend later
public class HabitController {

    @Autowired private HabitService habitService;
    @Autowired private UserRepository userRepository;

    // 🛠️ QUICK DEV SEED: Run this once to create a test user!
    @PostMapping("/seed-user")
    public ResponseEntity<User> seedUser() {
        if (userRepository.findByUsername("testuser").isPresent()) {
            return ResponseEntity.ok(userRepository.findByUsername("testuser").get());
        }
        User user = new User();
        user.setUsername("testuser");
        user.setEmail("test@example.com");
        user.setPasswordHash("hashed_password_here");
        return ResponseEntity.ok(userRepository.save(user));
    }

    // 1. Create a habit for a user
    @PostMapping
    public ResponseEntity<Habit> createHabit(@RequestBody Map<String, Object> payload) {
        Long userId = Long.valueOf(payload.get("userId").toString());
        String title = payload.get("title").toString();
        LocalTime reminderTime = LocalTime.parse(payload.get("reminderTime").toString());

        Habit habit = habitService.createHabit(userId, title, reminderTime);
        return ResponseEntity.ok(habit);
    }

    // 2. Fetch all active habits for a user
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Habit>> getHabits(@PathVariable Long userId) {
        return ResponseEntity.ok(habitService.getActiveHabits(userId));
    }

    // 3. Toggle completion for a habit on a specific date
    @PostMapping("/{habitId}/toggle")
    public ResponseEntity<DailyHabitLog> toggleHabit(
            @PathVariable Long habitId,
            @RequestParam Long userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        DailyHabitLog log = habitService.toggleHabitCompletion(userId, habitId, date);
        return ResponseEntity.ok(log);
    }
}
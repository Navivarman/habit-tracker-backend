package com.habittracker.backend.controller;

import com.habittracker.backend.dto.*;
import com.habittracker.backend.model.*;
import com.habittracker.backend.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.*;

@RestController
@RequestMapping("/api/history")
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class HistoryController {

    @Autowired private DailyHabitLogRepository logRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private HabitRepository habitRepository;

    // 1. GET FULL HEATMAP MATRIX MATRIX (Starts from user registration boundary)
    @GetMapping("/heatmap/{userId}")
    public ResponseEntity<?> getHeatmapMatrix(@PathVariable Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User profile context not found"));

        // Journey boundary: fallback to 90 days ago if registration metadata is missing
        LocalDate startDate = (user.getCreatedAt() != null) ? user.getCreatedAt().toLocalDate() : LocalDate.now().minusDays(90);
        LocalDate endDate = LocalDate.now();

        List<HeatmapStatusDTO> heatmapData = new ArrayList<>();
        LocalDate current = startDate;

        while (!current.isAfter(endDate)) {
            long total = logRepository.countTotalHabitsForUser(userId);
            long completed = logRepository.countCompletedHabitsForUserAndDate(userId, current);

            int intensity = 0;
            if (total > 0 && completed > 0) {
                double pct = (double) completed / total;
                if (pct <= 0.25) intensity = 1;
                else if (pct <= 0.50) intensity = 2;
                else if (pct <= 0.75) intensity = 3;
                else intensity = 4;
            }

            heatmapData.add(new HeatmapStatusDTO(current, completed, total, intensity));
            current = current.plusDays(1);
        }

        return ResponseEntity.ok(Map.of(
                "journeyStartDate", startDate,
                "matrix", heatmapData
        ));
    }

    // 2. GET DRILLDOWN DETAILED DAILY SNAPSHOT METRICS (On cell click)
    @GetMapping("/daily-detail")
    public ResponseEntity<DailyHistoryDetailDTO> getDailyDetail(
            @RequestParam Long userId,
            @RequestParam String dateStr) {

        LocalDate targetDate = LocalDate.parse(dateStr);

        // Pull actual individual habits and complete logs manually
        List<Habit> userHabits = habitRepository.findByUserId(userId);
        List<String> completed = new ArrayList<>();
        List<String> pending = new ArrayList<>();

        for (Habit h : userHabits) {
            boolean isDone = logRepository.findByHabitIdAndLogDate(h.getId(), targetDate)
                    .map(DailyHabitLog::isCompleted)
                    .orElse(false);

            if (isDone) {
                completed.add(h.getTitle());
            } else if (h.isActive()) {
                pending.add(h.getTitle());
            }
        }

        int total = completed.size() + pending.size();
        double pct = (total > 0) ? Math.round(((double) completed.size() / total) * 100.0) : 0.0;

        return ResponseEntity.ok(new DailyHistoryDetailDTO(completed, pending, total, pct));
    }
}
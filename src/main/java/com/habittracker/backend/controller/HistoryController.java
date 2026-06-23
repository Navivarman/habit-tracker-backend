package com.habittracker.backend.controller;

import com.habittracker.backend.dto.DailyHistoryDetailDTO;
import com.habittracker.backend.dto.HeatmapStatusDTO;
import com.habittracker.backend.model.DailyHabitLog;
import com.habittracker.backend.repository.DailyHabitLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/history")
@CrossOrigin(origins = "*")
public class HistoryController {

    @Autowired private DailyHabitLogRepository logRepository;

    // 🟢 🟡 🔴 Fetch complete year heatmap statuses aggregated dynamically
    @GetMapping("/heatmap/{userId}")
    public ResponseEntity<List<HeatmapStatusDTO>> getHeatmapSummary(@PathVariable Long userId) {
        List<DailyHabitLog> allLogs = logRepository.findAll(); // Simple reference fetch

        // Group logs by date
        Map<LocalDate, List<DailyHabitLog>> groupedByDate = allLogs.stream()
                .filter(log -> log.getUser().getId().equals(userId))
                .collect(Collectors.groupingBy(DailyHabitLog::getLogDate));

        List<HeatmapStatusDTO> summaryList = new ArrayList<>();

        groupedByDate.forEach((date, logs) -> {
            long total = logs.size();
            long completed = logs.stream().filter(DailyHabitLog::isCompleted).count();

            String status = "RED";
            if (completed == total && total > 0) status = "GREEN";
            else if (completed > 0) status = "YELLOW";

            summaryList.add(new HeatmapStatusDTO(date, status));
        });

        return ResponseEntity.ok(summaryList);
    }

    // Click square breakdown details fetching engine
    @GetMapping("/detail/{userId}")
    public ResponseEntity<DailyHistoryDetailDTO> getDailyDetails(
            @PathVariable Long userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        List<DailyHabitLog> logs = logRepository.findByUserIdAndLogDate(userId, date);
        long completed = logs.stream().filter(DailyHabitLog::isCompleted).count();
        long total = logs.size();

        DailyHistoryDetailDTO detail = new DailyHistoryDetailDTO();
        detail.setLogs(logs);
        detail.setTotalScheduled(total);
        detail.setTotalCompleted(completed);
        detail.setCompletionPercentage(total > 0 ? ((double) completed / total) * 100 : 0.0);

        return ResponseEntity.ok(detail);
    }
}
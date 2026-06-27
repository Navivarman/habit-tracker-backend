package com.habittracker.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class HeatmapStatusDTO {
    private LocalDate date;
    private long completedTasks;
    private long totalTasks;
    private int intensityOffset; // 0 = empty, 1-4 = color intensity levels
}
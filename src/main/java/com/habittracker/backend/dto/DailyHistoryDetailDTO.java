package com.habittracker.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DailyHistoryDetailDTO {
    private List<String> completedTasks;
    private List<String> pendingTasks;
    private int totalTasks;
    private double completionPercentage;
}
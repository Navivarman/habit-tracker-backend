package com.habittracker.backend.dto;

import com.habittracker.backend.model.DailyHabitLog;
import lombok.Data;
import java.util.List;

@Data
public class DailyHistoryDetailDTO {
    private List<DailyHabitLog> logs;
    private long totalCompleted;
    private long totalScheduled;
    private double completionPercentage;
}
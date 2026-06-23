package com.habittracker.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.time.LocalDate;

@Data
@AllArgsConstructor
public class HeatmapStatusDTO {
    private LocalDate date;
    private String status; // GREEN, YELLOW, RED
}
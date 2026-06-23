package com.habittracker.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class StepUpdatePayload {
    private Long roomId;
    private Long userId;
    private int newStep;
}
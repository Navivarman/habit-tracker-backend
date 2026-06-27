package com.habittracker.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TaskUpdatePayload {
    private Long roomId;
    private String action; // "CREATED" or "TOGGLED"
    private Long taskId;
    private String title;
    private boolean completed;
}
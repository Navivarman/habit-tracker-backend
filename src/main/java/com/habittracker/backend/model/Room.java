package com.habittracker.backend.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "rooms")
@Data
@NoArgsConstructor
public class Room {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(unique = true, nullable = false, length = 10)
    private String inviteCode;

    @ManyToOne
    @JoinColumn(name = "created_by")
    private User creator;

    @Column(nullable = false)
    private LocalDate targetMonthYear; // e.g., 2026-06-01 representing June 2026

    private LocalDateTime createdAt = LocalDateTime.now();
}
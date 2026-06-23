package com.habittracker.backend.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;

@Entity
@Table(name = "badges")
@Data
@NoArgsConstructor
public class Badge {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String badgeType; // "PERFECT_CONSISTENCY" or "MONTHLY_ROOM_WINNER"

    private String details; // e.g., "Winner of Room: June Fitness Challenge"

    private LocalDate awardedForMonth; // Holds the date locked to the target month
}
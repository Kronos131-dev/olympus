package com.kronos.olympus.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "user_metrics")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserMetrics {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private Double weightKg;

    @Column(nullable = false)
    private Integer calorieGoal;

    @Column(nullable = false)
    private LocalDate recordedDate;
}

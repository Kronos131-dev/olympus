package com.kronos.olympus.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.List;

@Entity
@Table(name = "daily_logs", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "target_date"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DailyLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false) // Hibernate traduira "targetDate" en "target_date" automatiquement (PhysicalNamingStrategy)
    private LocalDate targetDate;

    // Totaux calculés et stockés pour des requêtes d'analytics rapides
    @Builder.Default
    @Column(nullable = false)
    private Double totalKcal = 0.0;

    @Builder.Default
    @Column(nullable = false)
    private Double totalProteins = 0.0;

    @Builder.Default
    @Column(nullable = false)
    private Double totalCarbs = 0.0;

    @Builder.Default
    @Column(nullable = false)
    private Double totalFats = 0.0;

    @OneToMany(mappedBy = "dailyLog", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<LogEntry> entries;
}

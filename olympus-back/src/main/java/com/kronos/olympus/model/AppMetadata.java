package com.kronos.olympus.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "app_metadata")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppMetadata {
    @Id
    @Column(name = "key", length = 64)
    private String key;

    @Column(name = "value", nullable = false)
    private String value;
}

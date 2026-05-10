package com.edu.pcmaster.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "ml_model_metadata")
public class MLModelMetadata {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String modelName;

    @Column(nullable = false)
    private String modelVersion;

    private String modelPath;
    private Double accuracy;

    private LocalDateTime trainedAt;

    private Boolean isActive;
}

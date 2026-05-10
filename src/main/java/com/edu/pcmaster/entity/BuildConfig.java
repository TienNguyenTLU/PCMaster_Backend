package com.edu.pcmaster.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "build_configs")
public class BuildConfig {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    private String configName;

    @OneToMany(mappedBy = "buildConfig", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<BuildComponent> components;

    private Double bottleneckScore;
    private String bottleneckComponent;
    
    @Column(columnDefinition = "text")
    private String advice;
    
    @Column(precision = 19, scale = 4)
    private BigDecimal totalPrice;
    
    private Integer viewCount;
    private Boolean isPublic;

    @CreationTimestamp
    private LocalDateTime createdAt;
}

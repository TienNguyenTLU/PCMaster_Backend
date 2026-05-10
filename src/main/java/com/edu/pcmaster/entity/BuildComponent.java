package com.edu.pcmaster.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "build_components")
public class BuildComponent {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "build_config_id", nullable = false)
    private BuildConfig buildConfig;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cpu_id")
    private Product cpu;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "gpu_id")
    private Product gpu;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "motherboard_id")
    private Product motherboard;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ram_id")
    private Product ram;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "storage_id")
    private Product storage;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "psu_id")
    private Product psu;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cpu_cooler_id")
    private Product cpuCooler;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pc_case_id")
    private Product pcCase;
}

package com.edu.pcmaster.models;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "bottleneck_profiles")
@Getter
@Setter
public class BottleneckProfile {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "cpu_product_id")
	private Product cpuProduct;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "gpu_product_id")
	private Product gpuProduct;

	@Column(length = 10)
	private String resolution;

	@Column(name = "bottleneck_percent", precision = 5, scale = 2)
	private BigDecimal bottleneckPercent;

	@Enumerated(EnumType.STRING)
	@Column(name = "bottleneck_side", length = 10)
	private BottleneckSide bottleneckSide;

	@Column(name = "fps_estimate")
	private Integer fpsEstimate;
}


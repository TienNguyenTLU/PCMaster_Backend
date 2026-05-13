package com.edu.pcmaster.models;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "pc_builds")
@Getter
@Setter
public class PcBuild {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id")
	private User user;

	@Column(nullable = false, length = 100)
	private String name;

	@Column(name = "total_price", precision = 12, scale = 2)
	private BigDecimal totalPrice;

	@Column(name = "total_power")
	private Integer totalPower;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@OneToMany(mappedBy = "pcBuild", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<PcBuildItem> items = new ArrayList<>();

	@PrePersist
	void onCreate() {
		createdAt = Instant.now();
	}
}


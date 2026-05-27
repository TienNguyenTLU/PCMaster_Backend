package com.edu.pcmaster.models;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "promotions")
@Getter
@Setter
public class Promotion {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 255)
	private String name;

	@Column(nullable = false, unique = true, length = 255)
	private String slug;

	@Column(columnDefinition = "text")
	private String description;

	@Column(name = "banner_url", length = 500)
	private String bannerUrl;

	@Column(name = "discount_percent", nullable = false)
	private Integer discountPercent;

	@Column(name = "start_date", nullable = false)
	private Instant startDate;

	@Column(name = "end_date", nullable = false)
	private Instant endDate;

	@Column(nullable = false)
	private Boolean active = true;

	@ManyToMany(fetch = FetchType.LAZY)
	@JoinTable(
		name = "product_promotions",
		joinColumns = @JoinColumn(name = "promotion_id"),
		inverseJoinColumns = @JoinColumn(name = "product_id")
	)
	private List<Product> products = new ArrayList<>();

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@PrePersist
	void onCreate() {
		createdAt = Instant.now();
		if (active == null) {
			active = true;
		}
	}
}

package com.edu.pcmaster.models;

import java.math.BigDecimal;
import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "coupons")
@Getter
@Setter
public class Coupon {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true, length = 100)
	private String code;

	@Column(name = "discount_type", nullable = false, length = 50)
	private String discountType; // "PERCENTAGE" or "FIXED_AMOUNT"

	@Column(name = "discount_value", nullable = false, precision = 12, scale = 2)
	private BigDecimal discountValue;

	@Column(name = "min_order_amount", precision = 12, scale = 2)
	private BigDecimal minOrderAmount = BigDecimal.ZERO;

	@Column(name = "max_discount_amount", precision = 12, scale = 2)
	private BigDecimal maxDiscountAmount;

	@Column(name = "start_date", nullable = false)
	private Instant startDate;

	@Column(name = "end_date", nullable = false)
	private Instant endDate;

	@Column(name = "usage_limit")
	private Integer usageLimit;

	@Column(name = "usage_count", nullable = false)
	private Integer usageCount = 0;

	@Column(nullable = false)
	private Boolean active = true;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@PrePersist
	void onCreate() {
		createdAt = Instant.now();
		if (usageCount == null) {
			usageCount = 0;
		}
		if (active == null) {
			active = true;
		}
		if (minOrderAmount == null) {
			minOrderAmount = BigDecimal.ZERO;
		}
	}
}

package com.edu.pcmaster.dto.coupon;

import java.math.BigDecimal;
import java.time.Instant;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CouponRequest(
		@NotBlank String code,
		@NotBlank String discountType, // "PERCENTAGE" or "FIXED_AMOUNT"
		@NotNull @Positive BigDecimal discountValue,
		BigDecimal minOrderAmount,
		BigDecimal maxDiscountAmount,
		@NotNull Instant startDate,
		@NotNull Instant endDate,
		Integer usageLimit,
		Boolean active
) {}

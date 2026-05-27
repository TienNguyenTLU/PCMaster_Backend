package com.edu.pcmaster.dto.coupon;

import java.math.BigDecimal;
import java.time.Instant;

public record CouponResponse(
		Long id,
		String code,
		String discountType,
		BigDecimal discountValue,
		BigDecimal minOrderAmount,
		BigDecimal maxDiscountAmount,
		Instant startDate,
		Instant endDate,
		Integer usageLimit,
		Integer usageCount,
		Boolean active,
		Instant createdAt
) {}

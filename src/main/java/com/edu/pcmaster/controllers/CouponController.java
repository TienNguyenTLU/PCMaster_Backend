package com.edu.pcmaster.controllers;

import org.springframework.web.bind.annotation.*;
import com.edu.pcmaster.models.*;
import com.edu.pcmaster.repositories.*;
import com.edu.pcmaster.common.exception.BadRequestException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/coupons")
public class CouponController {
	private final CouponRepository couponRepository;

	public CouponController(CouponRepository couponRepository) {
		this.couponRepository = couponRepository;
	}

	@GetMapping("/validate")
	public Map<String, Object> validateCoupon(@RequestParam String code, @RequestParam BigDecimal amount) {
		Coupon coupon = couponRepository.findByCodeIgnoreCase(code.trim())
				.orElseThrow(() -> new BadRequestException("Mã giảm giá không tồn tại."));

		if (!coupon.getActive() || coupon.getStartDate().isAfter(Instant.now()) || coupon.getEndDate().isBefore(Instant.now())) {
			throw new BadRequestException("Mã giảm giá đã hết hạn hoặc chưa kích hoạt.");
		}

		if (coupon.getUsageLimit() != null && coupon.getUsageCount() >= coupon.getUsageLimit()) {
			throw new BadRequestException("Mã giảm giá đã hết lượt sử dụng.");
		}

		if (amount.compareTo(coupon.getMinOrderAmount()) < 0) {
			throw new BadRequestException("Đơn hàng chưa đạt giá trị tối thiểu " + coupon.getMinOrderAmount() + "đ để áp dụng mã.");
		}

		BigDecimal discount = BigDecimal.ZERO;
		if ("PERCENTAGE".equalsIgnoreCase(coupon.getDiscountType())) {
			discount = amount.multiply(coupon.getDiscountValue())
					.divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
			if (coupon.getMaxDiscountAmount() != null && discount.compareTo(coupon.getMaxDiscountAmount()) > 0) {
				discount = coupon.getMaxDiscountAmount();
			}
		} else if ("FIXED_AMOUNT".equalsIgnoreCase(coupon.getDiscountType())) {
			discount = coupon.getDiscountValue();
		}

		if (discount.compareTo(amount) > 0) {
			discount = amount;
		}

		Map<String, Object> response = new HashMap<>();
		response.put("valid", true);
		response.put("code", coupon.getCode());
		response.put("discountAmount", discount);
		response.put("discountType", coupon.getDiscountType());
		response.put("discountValue", coupon.getDiscountValue());
		return response;
	}
}

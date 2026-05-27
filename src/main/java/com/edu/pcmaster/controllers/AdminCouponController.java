package com.edu.pcmaster.controllers;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import com.edu.pcmaster.dto.coupon.*;
import com.edu.pcmaster.models.*;
import com.edu.pcmaster.repositories.*;
import com.edu.pcmaster.common.exception.ResourceNotFoundException;
import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/admin/coupons")
@PreAuthorize("hasRole('ADMIN')")
public class AdminCouponController {
	private final CouponRepository couponRepository;

	public AdminCouponController(CouponRepository couponRepository) {
		this.couponRepository = couponRepository;
	}

	@GetMapping
	public List<CouponResponse> listAll() {
		return couponRepository.findAll().stream()
				.map(this::toResponse)
				.toList();
	}

	@GetMapping("/{id}")
	public CouponResponse detail(@PathVariable Long id) {
		Coupon coupon = couponRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Coupon not found"));
		return toResponse(coupon);
	}

	@PostMapping
	public CouponResponse create(@Valid @RequestBody CouponRequest request) {
		Coupon coupon = new Coupon();
		coupon.setCode(request.code().toUpperCase().trim());
		coupon.setDiscountType(request.discountType());
		coupon.setDiscountValue(request.discountValue());
		coupon.setMinOrderAmount(request.minOrderAmount());
		coupon.setMaxDiscountAmount(request.maxDiscountAmount());
		coupon.setStartDate(request.startDate());
		coupon.setEndDate(request.endDate());
		coupon.setUsageLimit(request.usageLimit());
		if (request.active() != null) {
			coupon.setActive(request.active());
		}
		return toResponse(couponRepository.save(coupon));
	}

	@PutMapping("/{id}")
	public CouponResponse update(@PathVariable Long id, @Valid @RequestBody CouponRequest request) {
		Coupon coupon = couponRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Coupon not found"));
		coupon.setCode(request.code().toUpperCase().trim());
		coupon.setDiscountType(request.discountType());
		coupon.setDiscountValue(request.discountValue());
		coupon.setMinOrderAmount(request.minOrderAmount());
		coupon.setMaxDiscountAmount(request.maxDiscountAmount());
		coupon.setStartDate(request.startDate());
		coupon.setEndDate(request.endDate());
		coupon.setUsageLimit(request.usageLimit());
		if (request.active() != null) {
			coupon.setActive(request.active());
		}
		return toResponse(couponRepository.save(coupon));
	}

	@DeleteMapping("/{id}")
	public void delete(@PathVariable Long id) {
		Coupon coupon = couponRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Coupon not found"));
		couponRepository.delete(coupon);
	}

	private CouponResponse toResponse(Coupon c) {
		return new CouponResponse(
				c.getId(),
				c.getCode(),
				c.getDiscountType(),
				c.getDiscountValue(),
				c.getMinOrderAmount(),
				c.getMaxDiscountAmount(),
				c.getStartDate(),
				c.getEndDate(),
				c.getUsageLimit(),
				c.getUsageCount(),
				c.getActive(),
				c.getCreatedAt()
		);
	}
}

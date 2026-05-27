package com.edu.pcmaster.repositories;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.edu.pcmaster.models.Coupon;

public interface CouponRepository extends JpaRepository<Coupon, Long> {
	Optional<Coupon> findByCodeIgnoreCase(String code);
}

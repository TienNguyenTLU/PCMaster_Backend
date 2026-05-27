package com.edu.pcmaster.repositories;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.edu.pcmaster.models.Promotion;

public interface PromotionRepository extends JpaRepository<Promotion, Long> {
	Optional<Promotion> findBySlug(String slug);

	@Query("SELECT p FROM Promotion p WHERE p.active = true AND p.startDate <= :now AND p.endDate >= :now")
	List<Promotion> findActivePromotions(@Param("now") Instant now);

	@Query("SELECT prod.id, p.discountPercent FROM Promotion p JOIN p.products prod WHERE p.active = true AND p.startDate <= :now AND p.endDate >= :now")
	List<Object[]> findActiveProductDiscounts(@Param("now") Instant now);
}

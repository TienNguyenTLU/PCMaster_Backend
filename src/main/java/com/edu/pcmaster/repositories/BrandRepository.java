package com.edu.pcmaster.repositories;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.edu.pcmaster.models.Brand;

public interface BrandRepository extends JpaRepository<Brand, Long> {
	Optional<Brand> findByNameIgnoreCase(String name);

	@org.springframework.data.jpa.repository.Modifying
	@org.springframework.data.jpa.repository.Query(value = "DELETE FROM supplier_brands WHERE brand_id = :brandId", nativeQuery = true)
	void deleteFromSupplierBrands(@org.springframework.data.repository.query.Param("brandId") Long brandId);
}

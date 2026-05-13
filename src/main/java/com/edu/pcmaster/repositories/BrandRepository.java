package com.edu.pcmaster.repositories;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.edu.pcmaster.models.Brand;

public interface BrandRepository extends JpaRepository<Brand, Long> {
	Optional<Brand> findByNameIgnoreCase(String name);
}

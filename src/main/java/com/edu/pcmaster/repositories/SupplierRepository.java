package com.edu.pcmaster.repositories;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.edu.pcmaster.models.Supplier;

public interface SupplierRepository extends JpaRepository<Supplier, Long> {
	Optional<Supplier> findByNameIgnoreCase(String name);
}

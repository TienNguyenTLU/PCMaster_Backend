package com.edu.pcmaster.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.edu.pcmaster.models.InventoryBatch;
import com.edu.pcmaster.models.Product;

public interface InventoryBatchRepository extends JpaRepository<InventoryBatch, Long> {
	List<InventoryBatch> findByProductAndRemainingQuantityGreaterThanOrderByImportedAtAsc(Product product, int remaining);
}


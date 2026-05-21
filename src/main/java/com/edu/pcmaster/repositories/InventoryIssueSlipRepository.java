package com.edu.pcmaster.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.edu.pcmaster.models.InventoryIssueSlip;
import com.edu.pcmaster.models.Order;

public interface InventoryIssueSlipRepository extends JpaRepository<InventoryIssueSlip, Long> {
	List<InventoryIssueSlip> findAllByOrderByCreatedAtDesc();

	Page<InventoryIssueSlip> findAllByOrderByCreatedAtDesc(Pageable pageable);

	Optional<InventoryIssueSlip> findByOrder(Order order);
}

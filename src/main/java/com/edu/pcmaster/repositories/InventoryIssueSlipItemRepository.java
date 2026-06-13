package com.edu.pcmaster.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import com.edu.pcmaster.models.InventoryIssueSlipItem;

public interface InventoryIssueSlipItemRepository extends JpaRepository<InventoryIssueSlipItem, Long> {
}

package com.edu.pcmaster.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import com.edu.pcmaster.models.PurchaseOrderItem;

public interface PurchaseOrderItemRepository extends JpaRepository<PurchaseOrderItem, Long> {
}


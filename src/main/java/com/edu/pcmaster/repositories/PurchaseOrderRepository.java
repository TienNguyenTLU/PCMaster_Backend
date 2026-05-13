package com.edu.pcmaster.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import com.edu.pcmaster.models.PurchaseOrder;

public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, Long> {
}


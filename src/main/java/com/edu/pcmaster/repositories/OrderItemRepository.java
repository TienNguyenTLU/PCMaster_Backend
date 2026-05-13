package com.edu.pcmaster.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import com.edu.pcmaster.models.OrderItem;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
}


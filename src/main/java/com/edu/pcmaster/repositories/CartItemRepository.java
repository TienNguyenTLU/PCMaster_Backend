package com.edu.pcmaster.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.edu.pcmaster.models.CartItem;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Long> {
}

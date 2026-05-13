package com.edu.pcmaster.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.edu.pcmaster.models.Order;
import com.edu.pcmaster.models.User;

public interface OrderRepository extends JpaRepository<Order, Long> {
	List<Order> findByUser(User user);
}


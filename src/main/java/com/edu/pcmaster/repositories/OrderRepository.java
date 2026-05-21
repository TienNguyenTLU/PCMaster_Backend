package com.edu.pcmaster.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.edu.pcmaster.models.Order;
import com.edu.pcmaster.models.OrderStatus;
import com.edu.pcmaster.models.User;

public interface OrderRepository extends JpaRepository<Order, Long> {
	List<Order> findByUser(User user);

	List<Order> findAllByOrderByCreatedAtDesc();

	List<Order> findByStatusOrderByCreatedAtDesc(OrderStatus status);

	@Query("SELECT COUNT(o) FROM Order o WHERE o.status = :status")
	long countByStatus(OrderStatus status);
}

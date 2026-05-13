package com.edu.pcmaster.services;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.edu.pcmaster.common.exception.BadRequestException;
import com.edu.pcmaster.common.exception.ResourceNotFoundException;
import com.edu.pcmaster.dto.order.OrderItemRequest;
import com.edu.pcmaster.dto.order.OrderRequest;
import com.edu.pcmaster.models.InventoryBatch;
import com.edu.pcmaster.models.Order;
import com.edu.pcmaster.models.OrderItem;
import com.edu.pcmaster.models.OrderStatus;
import com.edu.pcmaster.models.Product;
import com.edu.pcmaster.models.User;
import com.edu.pcmaster.repositories.InventoryBatchRepository;
import com.edu.pcmaster.repositories.OrderRepository;
import com.edu.pcmaster.repositories.ProductRepository;

@Service
public class OrderService {
	private final OrderRepository orderRepository;
	private final ProductRepository productRepository;
	private final InventoryBatchRepository inventoryBatchRepository;

	public OrderService(OrderRepository orderRepository,
					ProductRepository productRepository,
					InventoryBatchRepository inventoryBatchRepository) {
		this.orderRepository = orderRepository;
		this.productRepository = productRepository;
		this.inventoryBatchRepository = inventoryBatchRepository;
	}

	public List<Order> findByUser(User user) {
		return orderRepository.findByUser(user);
	}

	public Order getById(Long id) {
		return orderRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Order not found"));
	}

	public Order getByIdForUser(Long id, User user) {
		Order order = getById(id);
		if (order.getUser() == null || !order.getUser().getId().equals(user.getId())) {
			throw new BadRequestException("Access denied");
		}
		return order;
	}

	@Transactional
	public Order create(OrderRequest request, User user) {
		if (request.items() == null || request.items().isEmpty()) {
			throw new BadRequestException("Order items are required");
		}
		Order order = new Order();
		order.setUser(user);
		order.setStatus(OrderStatus.PENDING);

		BigDecimal totalAmount = BigDecimal.ZERO;

		for (OrderItemRequest itemRequest : request.items()) {
			Product product = productRepository.findById(itemRequest.productId())
					.orElseThrow(() -> new ResourceNotFoundException("Product not found"));
			if (product.getStock() < itemRequest.quantity()) {
				throw new BadRequestException("Insufficient stock for product " + product.getId());
			}

			int remaining = itemRequest.quantity();
			BigDecimal totalCost = BigDecimal.ZERO;
			List<InventoryBatch> batches = inventoryBatchRepository
					.findByProductAndRemainingQuantityGreaterThanOrderByImportedAtAsc(product, 0);

			for (InventoryBatch batch : batches) {
				if (remaining <= 0) {
					break;
				}
				int take = Math.min(remaining, batch.getRemainingQuantity());
				batch.setRemainingQuantity(batch.getRemainingQuantity() - take);
				inventoryBatchRepository.save(batch);
				totalCost = totalCost.add(batch.getImportPrice().multiply(BigDecimal.valueOf(take)));
				remaining -= take;
			}

			if (remaining > 0) {
				throw new BadRequestException("Inventory batches not enough for product " + product.getId());
			}

			BigDecimal costPrice = totalCost.divide(BigDecimal.valueOf(itemRequest.quantity()), 2, RoundingMode.HALF_UP);
			OrderItem orderItem = new OrderItem();
			orderItem.setOrder(order);
			orderItem.setProduct(product);
			orderItem.setQuantity(itemRequest.quantity());
			orderItem.setSellingPrice(product.getPrice());
			orderItem.setCostPrice(costPrice);
			order.getItems().add(orderItem);

			product.setStock(product.getStock() - itemRequest.quantity());
			productRepository.save(product);

			totalAmount = totalAmount.add(product.getPrice().multiply(BigDecimal.valueOf(itemRequest.quantity())));
		}

		order.setTotalAmount(totalAmount);
		order.setStatus(OrderStatus.CONFIRMED);
		return orderRepository.save(order);
	}
}




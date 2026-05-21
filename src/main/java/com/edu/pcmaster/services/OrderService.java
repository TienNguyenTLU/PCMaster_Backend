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
import com.edu.pcmaster.models.DeliveryType;
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
	private final OrderDocumentService orderDocumentService;
	private final MediaService mediaService;

	public OrderService(OrderRepository orderRepository,
						ProductRepository productRepository,
						InventoryBatchRepository inventoryBatchRepository,
						OrderDocumentService orderDocumentService,
						MediaService mediaService) {
		this.orderRepository = orderRepository;
		this.productRepository = productRepository;
		this.inventoryBatchRepository = inventoryBatchRepository;
		this.orderDocumentService = orderDocumentService;
		this.mediaService = mediaService;
	}

	// ── Customer queries ────────────────────────────────────────────────────────

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

	// ── Admin queries ───────────────────────────────────────────────────────────

	public List<Order> findAll() {
		return orderRepository.findAllByOrderByCreatedAtDesc();
	}

	// ── Create order — status DRAFT, no stock deduction yet ────────────────────

	@Transactional
	public Order create(OrderRequest request, User user) {
		if (request.items() == null || request.items().isEmpty()) {
			throw new BadRequestException("Order items are required");
		}
		if (request.deliveryType() == null) {
			throw new BadRequestException("Delivery type is required");
		}
		if (request.deliveryType() == DeliveryType.HOME_DELIVERY) {
			if (request.recipientName() == null || request.recipientName().isBlank()) {
				throw new BadRequestException("Recipient name is required for home delivery");
			}
			if (request.recipientPhone() == null || request.recipientPhone().isBlank()) {
				throw new BadRequestException("Recipient phone is required for home delivery");
			}
			if (request.shippingAddress() == null || request.shippingAddress().isBlank()) {
				throw new BadRequestException("Shipping address is required for home delivery");
			}
		}

		Order order = new Order();
		order.setUser(user);
		order.setStatus(OrderStatus.DRAFT);
		order.setDeliveryType(request.deliveryType());
		order.setRecipientName(request.recipientName());
		order.setRecipientPhone(request.recipientPhone());
		order.setShippingAddress(request.shippingAddress());

		BigDecimal totalAmount = BigDecimal.ZERO;

		for (OrderItemRequest itemRequest : request.items()) {
			Product product = productRepository.findById(itemRequest.productId())
					.orElseThrow(() -> new ResourceNotFoundException("Product not found"));
			if (product.getStock() < itemRequest.quantity()) {
				throw new BadRequestException("Insufficient stock for product: " + product.getName());
			}

			OrderItem orderItem = new OrderItem();
			orderItem.setOrder(order);
			orderItem.setProduct(product);
			orderItem.setQuantity(itemRequest.quantity());
			orderItem.setSellingPrice(product.getPrice());
			order.getItems().add(orderItem);

			totalAmount = totalAmount.add(product.getPrice().multiply(BigDecimal.valueOf(itemRequest.quantity())));
		}

		order.setTotalAmount(totalAmount);
		return orderRepository.save(order);
	}

	// ── Admin: confirm order — deduct stock, generate + upload DOCX ────────────

	@Transactional
	public Order confirm(Long id) {
		Order order = getById(id);
		if (order.getStatus() != OrderStatus.DRAFT) {
			throw new BadRequestException("Only DRAFT orders can be confirmed");
		}

		for (OrderItem orderItem : order.getItems()) {
			Product product = orderItem.getProduct();
			int quantityToDeduct = orderItem.getQuantity();

			if (product.getStock() < quantityToDeduct) {
				throw new BadRequestException("Không đủ hàng trong kho cho sản phẩm: " + product.getName());
			}

			// FIFO: Find oldest batches with remaining stock
			List<InventoryBatch> batches = inventoryBatchRepository.findByProductAndRemainingQuantityGreaterThanOrderByImportedAtAsc(product, 0);

			BigDecimal weightedAverageCost = BigDecimal.ZERO;
			int totalQuantityFromBatches = 0;

			for (InventoryBatch batch : batches) {
				int quantityFromThisBatch = Math.min(quantityToDeduct, batch.getRemainingQuantity());
				
				weightedAverageCost = weightedAverageCost.add(batch.getImportPrice().multiply(BigDecimal.valueOf(quantityFromThisBatch)));
				totalQuantityFromBatches += quantityFromThisBatch;

				batch.setRemainingQuantity(batch.getRemainingQuantity() - quantityFromThisBatch);
				inventoryBatchRepository.save(batch);

				quantityToDeduct -= quantityFromThisBatch;
				if (quantityToDeduct == 0) {
					break;
				}
			}

			if (quantityToDeduct > 0) {
				throw new BadRequestException("Lỗi logic: Không đủ số lượng trong các lô hàng tồn kho cho sản phẩm: " + product.getName());
			}
			
			BigDecimal averageCost = weightedAverageCost.divide(BigDecimal.valueOf(totalQuantityFromBatches), 2, RoundingMode.HALF_UP);
			orderItem.setCostPrice(averageCost);

			// Update product's total stock
			product.setStock(product.getStock() - orderItem.getQuantity());
			productRepository.save(product);
		}

		order.setStatus(OrderStatus.CONFIRMED);
		return orderRepository.save(order);
	}


	// ── Admin: update status (CONFIRMED → SHIPPED → DELIVERED) ─────────────────

	@Transactional
	public Order updateStatus(Long id, OrderStatus newStatus) {
		Order order = getById(id);
		OrderStatus current = order.getStatus();

		// Allowed transitions
		boolean valid = switch (current) {
			case DRAFT -> newStatus == OrderStatus.CANCELLED;
			case CONFIRMED -> newStatus == OrderStatus.SHIPPED || newStatus == OrderStatus.CANCELLED;
			case SHIPPED -> newStatus == OrderStatus.DELIVERED;
			default -> false;
		};

		if (!valid) {
			throw new BadRequestException("Cannot transition from " + current + " to " + newStatus);
		}

		order.setStatus(newStatus);
		return orderRepository.save(order);
	}
}

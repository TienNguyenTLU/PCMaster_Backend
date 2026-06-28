package com.edu.pcmaster.services;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.edu.pcmaster.common.exception.BadRequestException;
import com.edu.pcmaster.common.exception.ResourceNotFoundException;
import com.edu.pcmaster.models.InventoryBatch;
import com.edu.pcmaster.models.InventoryIssueSlip;
import com.edu.pcmaster.models.InventoryIssueSlipItem;
import com.edu.pcmaster.models.Order;
import com.edu.pcmaster.models.OrderItem;
import com.edu.pcmaster.models.OrderStatus;
import com.edu.pcmaster.models.Product;
import com.edu.pcmaster.repositories.InventoryBatchRepository;
import com.edu.pcmaster.repositories.InventoryIssueSlipRepository;
import com.edu.pcmaster.repositories.OrderRepository;
import com.edu.pcmaster.repositories.ProductRepository;

@Service
public class InventoryService {

	private final InventoryBatchRepository inventoryBatchRepository;
	private final InventoryIssueSlipRepository inventoryIssueSlipRepository;
	private final OrderRepository orderRepository;
	private final ProductRepository productRepository;
	private final OrderDocumentService orderDocumentService;
	private final MediaService mediaService;

	public InventoryService(InventoryBatchRepository inventoryBatchRepository,
							InventoryIssueSlipRepository inventoryIssueSlipRepository,
							OrderRepository orderRepository,
							ProductRepository productRepository,
							OrderDocumentService orderDocumentService,
							MediaService mediaService) {
		this.inventoryBatchRepository = inventoryBatchRepository;
		this.inventoryIssueSlipRepository = inventoryIssueSlipRepository;
		this.orderRepository = orderRepository;
		this.productRepository = productRepository;
		this.orderDocumentService = orderDocumentService;
		this.mediaService = mediaService;
	}

	public List<InventoryBatch> getAllInventoryBatches() {
		return inventoryBatchRepository.findAll();
	}

	public Page<InventoryBatch> getInventoryBatchesPage(Pageable pageable) {
		return inventoryBatchRepository.findAll(pageable);
	}

	public List<InventoryIssueSlip> getAllIssueSlips() {
		return inventoryIssueSlipRepository.findAllByOrderByCreatedAtDesc();
	}

	public Page<InventoryIssueSlip> getIssueSlipsPage(Pageable pageable) {
		return inventoryIssueSlipRepository.findAllByOrderByCreatedAtDesc(pageable);
	}

	@Transactional
	public void updateInventoryPrices(Long batchId, BigDecimal importPrice, BigDecimal sellingPrice) {
		InventoryBatch batch = inventoryBatchRepository.findById(batchId)
				.orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lô hàng"));
		if (importPrice != null) {
			batch.setImportPrice(importPrice);
			inventoryBatchRepository.save(batch);
		}
		if (sellingPrice != null && batch.getProduct() != null) {
			Product product = batch.getProduct();
			product.setPrice(sellingPrice);
			productRepository.save(product);
		}
	}

	@Transactional
	public InventoryIssueSlip createIssueSlip(Long orderId) {
		Order order = orderRepository.findById(orderId)
				.orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng"));

		if (order.getStatus() != OrderStatus.CONFIRMED) {
			throw new BadRequestException("Chỉ đơn hàng đã DUYỆT (CONFIRMED) mới có thể tạo phiếu xuất kho");
		}

		if (inventoryIssueSlipRepository.findByOrder(order).isPresent()) {
			throw new BadRequestException("Phiếu xuất kho cho đơn hàng này đã tồn tại");
		}

		InventoryIssueSlip slip = new InventoryIssueSlip();
		slip.setOrder(order);
		slip.setCode("PXK-" + String.format("%05d", order.getId()));
		slip.setStatus("PENDING");

		return inventoryIssueSlipRepository.save(slip);
	}

	@Transactional
	public InventoryIssueSlip dispatchIssueSlip(Long slipId) {
		InventoryIssueSlip slip = inventoryIssueSlipRepository.findById(slipId)
				.orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phiếu xuất kho"));

		if ("COMPLETED".equals(slip.getStatus())) {
			throw new BadRequestException("Phiếu xuất kho này đã được hoàn thành");
		}

		Order order = slip.getOrder();

		
		for (OrderItem orderItem : order.getItems()) {
			Product product = orderItem.getProduct();
			int quantity = orderItem.getQuantity();

			if (product.getStock() < quantity) {
				throw new BadRequestException("Không đủ hàng trong kho cho sản phẩm: " + product.getName());
			}

			
			int remaining = quantity;
			BigDecimal totalCost = BigDecimal.ZERO;
			List<InventoryBatch> batches = inventoryBatchRepository
					.findByProductAndRemainingQuantityGreaterThanOrderByImportedAtAsc(product, 0);

			for (InventoryBatch batch : batches) {
				if (remaining <= 0) break;
				int take = Math.min(remaining, batch.getRemainingQuantity());
				batch.setRemainingQuantity(batch.getRemainingQuantity() - take);
				inventoryBatchRepository.save(batch);
				totalCost = totalCost.add(batch.getImportPrice().multiply(BigDecimal.valueOf(take)));
				remaining -= take;
			}

			if (remaining > 0) {
				// Fallback cho dữ liệu test: nếu không có đủ lô hàng, dùng giá bán sản phẩm (hoặc 0) làm giá gốc
				totalCost = totalCost.add(product.getPrice() != null ? product.getPrice().multiply(BigDecimal.valueOf(remaining)) : BigDecimal.ZERO);
				remaining = 0;
			}

			BigDecimal costPrice = totalCost.divide(BigDecimal.valueOf(quantity), 2, RoundingMode.HALF_UP);
			orderItem.setCostPrice(costPrice);

			product.setStock(product.getStock() - quantity);
			productRepository.save(product);
		}

		if (order.getDeliveryType() == com.edu.pcmaster.models.DeliveryType.SHOWROOM_PICKUP) {
			order.setStatus(OrderStatus.DELIVERED);
		} else {
			order.setStatus(OrderStatus.SHIPPED);
		}

		
		try {
			byte[] docBytes = orderDocumentService.generateExportDocument(order);
			String docUrl = mediaService.uploadRaw(docBytes,
					"PCMaster_Storage/Orders",
					"order_" + order.getId());
			order.setDocumentUrl(docUrl);
			slip.setDocumentUrl(docUrl);
		} catch (Exception e) {
			System.err.println("[InventoryService] XLSX generation/upload failed for order " + order.getId() + ": " + e.getMessage());
		}

		orderRepository.save(order);

		
		slip.setStatus("COMPLETED");
		slip.setCompletedAt(Instant.now());

		return inventoryIssueSlipRepository.save(slip);
	}

	@Transactional
	public java.math.BigDecimal deductStockFIFO(Product product, int quantity) {
		if (product.getStock() < quantity) {
			throw new BadRequestException("Không đủ hàng trong kho cho sản phẩm: " + product.getName());
		}

		
		int remaining = quantity;
		java.math.BigDecimal totalCost = java.math.BigDecimal.ZERO;
		List<InventoryBatch> batches = inventoryBatchRepository
				.findByProductAndRemainingQuantityGreaterThanOrderByImportedAtAsc(product, 0);

		for (InventoryBatch batch : batches) {
			if (remaining <= 0) break;
			int take = Math.min(remaining, batch.getRemainingQuantity());
			batch.setRemainingQuantity(batch.getRemainingQuantity() - take);
			inventoryBatchRepository.save(batch);
			totalCost = totalCost.add(batch.getImportPrice().multiply(java.math.BigDecimal.valueOf(take)));
			remaining -= take;
		}

		if (remaining > 0) {
			// Fallback cho dữ liệu test: nếu không có đủ lô hàng, dùng giá bán sản phẩm (hoặc 0) làm giá gốc
			totalCost = totalCost.add(product.getPrice() != null ? product.getPrice().multiply(java.math.BigDecimal.valueOf(remaining)) : java.math.BigDecimal.ZERO);
			remaining = 0;
		}

		product.setStock(product.getStock() - quantity);
		productRepository.save(product);
		
		return totalCost;
	}

	@Transactional
	public InventoryIssueSlip createManualIssueSlip(com.edu.pcmaster.dto.inventory.CreateManualIssueSlipRequest request) {
		if ("RETAIL_SALE".equals(request.exportReason())) {
			if (request.orderId() == null) {
				throw new BadRequestException("Xuất hàng bán lẻ yêu cầu chọn đơn hàng liên kết");
			}
			InventoryIssueSlip pendingSlip = createIssueSlip(request.orderId());
			pendingSlip.setExportReason("RETAIL_SALE");
			inventoryIssueSlipRepository.save(pendingSlip);
			return dispatchIssueSlip(pendingSlip.getId());
		}

		if (request.items() == null || request.items().isEmpty()) {
			throw new BadRequestException("Phiếu xuất phải chứa ít nhất một sản phẩm");
		}

		
		for (com.edu.pcmaster.dto.inventory.CreateManualIssueSlipRequest.ItemRequest itemReq : request.items()) {
			Product product = productRepository.findById(itemReq.productId())
					.orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sản phẩm với ID: " + itemReq.productId()));
			if (product.getStock() < itemReq.quantity()) {
				throw new BadRequestException("Không đủ hàng trong kho cho sản phẩm: " + product.getName() + " (Tồn kho: " + product.getStock() + ")");
			}
		}

		
		InventoryIssueSlip slip = new InventoryIssueSlip();
		slip.setStatus("COMPLETED");
		slip.setCompletedAt(Instant.now());
		slip.setExportReason(request.exportReason());
		
		slip.setCode("PXK-M-" + Instant.now().toEpochMilli());

		
		for (com.edu.pcmaster.dto.inventory.CreateManualIssueSlipRequest.ItemRequest itemReq : request.items()) {
			Product product = productRepository.findById(itemReq.productId()).get();
			
			deductStockFIFO(product, itemReq.quantity());

			InventoryIssueSlipItem item = new InventoryIssueSlipItem();
			item.setSlip(slip);
			item.setProduct(product);
			item.setQuantity(itemReq.quantity());
			slip.getItems().add(item);
		}

		return inventoryIssueSlipRepository.save(slip);
	}
}

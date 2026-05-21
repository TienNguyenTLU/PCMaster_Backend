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

		// Perform FIFO Stock Deduction
		for (OrderItem orderItem : order.getItems()) {
			Product product = orderItem.getProduct();
			int quantity = orderItem.getQuantity();

			if (product.getStock() < quantity) {
				throw new BadRequestException("Không đủ hàng trong kho cho sản phẩm: " + product.getName());
			}

			// FIFO deduction from inventory batches
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
				throw new BadRequestException("Các lô hàng không đủ số lượng cho sản phẩm: " + product.getName());
			}

			BigDecimal costPrice = totalCost.divide(BigDecimal.valueOf(quantity), 2, RoundingMode.HALF_UP);
			orderItem.setCostPrice(costPrice);

			product.setStock(product.getStock() - quantity);
			productRepository.save(product);
		}

		// Update order status to DELIVERED
		order.setStatus(OrderStatus.DELIVERED);

		// Generate export document DOCX and upload to Cloudinary
		try {
			byte[] docBytes = orderDocumentService.generateExportDocument(order);
			String docUrl = mediaService.uploadRaw(docBytes,
					"PCMaster_Storage/Orders",
					"order_" + order.getId());
			order.setDocumentUrl(docUrl);
			slip.setDocumentUrl(docUrl);
		} catch (Exception e) {
			System.err.println("[InventoryService] DOCX generation/upload failed for order " + order.getId() + ": " + e.getMessage());
		}

		orderRepository.save(order);

		// Complete Issue Slip
		slip.setStatus("COMPLETED");
		slip.setCompletedAt(Instant.now());

		return inventoryIssueSlipRepository.save(slip);
	}
}

package com.edu.pcmaster.services;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.edu.pcmaster.common.exception.BadRequestException;
import com.edu.pcmaster.common.exception.ResourceNotFoundException;
import com.edu.pcmaster.dto.purchase.PurchaseOrderItemRequest;
import com.edu.pcmaster.dto.purchase.PurchaseOrderRequest;
import com.edu.pcmaster.models.InventoryBatch;
import com.edu.pcmaster.models.Product;
import com.edu.pcmaster.models.PurchaseOrder;
import com.edu.pcmaster.models.PurchaseOrderItem;
import com.edu.pcmaster.models.PurchaseOrderStatus;
import com.edu.pcmaster.models.Supplier;
import com.edu.pcmaster.models.User;
import com.edu.pcmaster.repositories.InventoryBatchRepository;
import com.edu.pcmaster.repositories.ProductRepository;
import com.edu.pcmaster.repositories.PurchaseOrderRepository;
import com.edu.pcmaster.repositories.SupplierRepository;

@Service
public class PurchaseOrderService {
	private final PurchaseOrderRepository purchaseOrderRepository;
	private final SupplierRepository supplierRepository;
	private final ProductRepository productRepository;
	private final InventoryBatchRepository inventoryBatchRepository;

	public PurchaseOrderService(PurchaseOrderRepository purchaseOrderRepository,
							SupplierRepository supplierRepository,
							ProductRepository productRepository,
							InventoryBatchRepository inventoryBatchRepository) {
		this.purchaseOrderRepository = purchaseOrderRepository;
		this.supplierRepository = supplierRepository;
		this.productRepository = productRepository;
		this.inventoryBatchRepository = inventoryBatchRepository;
	}

	public List<PurchaseOrder> findAll() {
		return purchaseOrderRepository.findAll();
	}

	public PurchaseOrder getById(Long id) {
		return purchaseOrderRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Purchase order not found"));
	}

	@Transactional
	public PurchaseOrder create(PurchaseOrderRequest request, User createdBy) {
		Supplier supplier = supplierRepository.findById(request.supplierId())
				.orElseThrow(() -> new ResourceNotFoundException("Supplier not found"));
		if (request.items() == null || request.items().isEmpty()) {
			throw new BadRequestException("Purchase order items are required");
		}

		PurchaseOrder purchaseOrder = new PurchaseOrder();
		purchaseOrder.setSupplier(supplier);
		purchaseOrder.setCreatedBy(createdBy);
		purchaseOrder.setStatus(PurchaseOrderStatus.DRAFT);

		BigDecimal total = BigDecimal.ZERO;
		for (PurchaseOrderItemRequest itemRequest : request.items()) {
			Product product = productRepository.findById(itemRequest.productId())
					.orElseThrow(() -> new ResourceNotFoundException("Product not found"));

			PurchaseOrderItem item = new PurchaseOrderItem();
			item.setPurchaseOrder(purchaseOrder);
			item.setProduct(product);
			item.setQuantity(itemRequest.quantity());
			item.setImportPrice(itemRequest.importPrice());
			purchaseOrder.getItems().add(item);

			total = total.add(itemRequest.importPrice().multiply(BigDecimal.valueOf(itemRequest.quantity())));
		}
		purchaseOrder.setTotalAmount(total);
		return purchaseOrderRepository.save(purchaseOrder);
	}

	@Transactional
	public PurchaseOrder receive(Long id) {
		PurchaseOrder purchaseOrder = getById(id);
		if (purchaseOrder.getStatus() != PurchaseOrderStatus.DRAFT) {
			throw new BadRequestException("Purchase order already processed");
		}

		for (PurchaseOrderItem item : purchaseOrder.getItems()) {
			InventoryBatch batch = new InventoryBatch();
			batch.setProduct(item.getProduct());
			batch.setPurchaseOrderItem(item);
			batch.setQuantity(item.getQuantity());
			batch.setRemainingQuantity(item.getQuantity());
			batch.setImportPrice(item.getImportPrice());
			inventoryBatchRepository.save(batch);

			Product product = item.getProduct();
			product.setStock(product.getStock() + item.getQuantity());
			productRepository.save(product);
		}

		purchaseOrder.setStatus(PurchaseOrderStatus.RECEIVED);
		return purchaseOrderRepository.save(purchaseOrder);
	}
}



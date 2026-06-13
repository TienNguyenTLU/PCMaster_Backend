package com.edu.pcmaster.controllers;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.edu.pcmaster.dto.purchase.PurchaseOrderItemResponse;
import com.edu.pcmaster.dto.purchase.PurchaseOrderRequest;
import com.edu.pcmaster.dto.purchase.PurchaseOrderResponse;
import com.edu.pcmaster.models.PurchaseOrder;
import com.edu.pcmaster.services.CurrentUserService;
import com.edu.pcmaster.services.PurchaseOrderService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/admin/purchase-orders")
@PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
public class PurchaseOrderController {
	private final PurchaseOrderService purchaseOrderService;
	private final CurrentUserService currentUserService;

	public PurchaseOrderController(PurchaseOrderService purchaseOrderService, CurrentUserService currentUserService) {
		this.purchaseOrderService = purchaseOrderService;
		this.currentUserService = currentUserService;
	}

	@PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public PurchaseOrderResponse create(@Valid @RequestPart("data") PurchaseOrderRequest request,
								@RequestPart(value = "document", required = false) MultipartFile document) {
		return toResponse(purchaseOrderService.create(request, currentUserService.requireUser(), document));
	}

	@GetMapping
	public List<PurchaseOrderResponse> list() {
		return purchaseOrderService.findAll().stream()
				.map(this::toResponse)
				.toList();
	}

	@GetMapping("/{id}")
	public PurchaseOrderResponse detail(@PathVariable Long id) {
		return toResponse(purchaseOrderService.getById(id));
	}

	@PutMapping("/{id}/receive")
	public PurchaseOrderResponse receive(@PathVariable Long id, @org.springframework.web.bind.annotation.RequestBody(required = false) java.util.Map<Long, java.math.BigDecimal> newPrices) {
		return toResponse(purchaseOrderService.receive(id, newPrices));
	}

	private PurchaseOrderResponse toResponse(PurchaseOrder order) {
		return new PurchaseOrderResponse(
				order.getId(),
				order.getSupplier() == null ? null : order.getSupplier().getId(),
				order.getCreatedBy() == null ? null : order.getCreatedBy().getId(),
				order.getStatus(),
				order.getTotalAmount(),
				order.getCreatedAt(),
				order.getDocumentUrl(),
				order.getItems().stream()
						.map(item -> new PurchaseOrderItemResponse(
								item.getId(),
								item.getProduct() == null ? null : item.getProduct().getId(),
								item.getQuantity(),
								item.getImportPrice()
						))
						.toList()
		);
	}
}

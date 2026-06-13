package com.edu.pcmaster.controllers;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.edu.pcmaster.dto.inventory.InventoryBatchResponse;
import com.edu.pcmaster.dto.inventory.IssueSlipResponse;
import com.edu.pcmaster.models.InventoryBatch;
import com.edu.pcmaster.models.InventoryIssueSlip;
import com.edu.pcmaster.models.InventoryIssueSlipItem;
import com.edu.pcmaster.services.InventoryService;

@RestController
@RequestMapping("/api/admin/inventory")
@PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
public class AdminInventoryController {

	private final InventoryService inventoryService;

	public AdminInventoryController(InventoryService inventoryService) {
		this.inventoryService = inventoryService;
	}

	@GetMapping("/batches")
	public Page<InventoryBatchResponse> getBatches(@RequestParam(defaultValue = "0") int page,
												   @RequestParam(defaultValue = "10") int size) {
		Pageable pageable = PageRequest.of(page, size);
		return inventoryService.getInventoryBatchesPage(pageable)
				.map(b -> new InventoryBatchResponse(
						b.getId(),
						b.getProduct() != null ? b.getProduct().getId() : null,
						b.getProduct() != null ? b.getProduct().getName() : "N/A",
						b.getProduct() != null ? b.getProduct().getThumbnailUrl() : null,
						b.getQuantity(),
						b.getRemainingQuantity(),
						b.getImportPrice(),
						b.getProduct() != null ? b.getProduct().getPrice() : BigDecimal.ZERO,
						b.getImportedAt()
				));
	}

	@PutMapping("/batches/{id}/prices")
	public ResponseEntity<?> updatePrices(@PathVariable Long id,
										  @RequestParam(required = false) BigDecimal importPrice,
										  @RequestParam(required = false) BigDecimal sellingPrice) {
		inventoryService.updateInventoryPrices(id, importPrice, sellingPrice);
		return ResponseEntity.ok().build();
	}

	@GetMapping("/issue-slips")
	public Page<IssueSlipResponse> getIssueSlips(@RequestParam(defaultValue = "0") int page,
												 @RequestParam(defaultValue = "10") int size) {
		Pageable pageable = PageRequest.of(page, size);
		return inventoryService.getIssueSlipsPage(pageable)
				.map(this::toResponse);
	}

	@PostMapping("/issue-slips/create")
	public ResponseEntity<IssueSlipResponse> createIssueSlip(@RequestParam Long orderId) {
		return ResponseEntity.ok(toResponse(inventoryService.createIssueSlip(orderId)));
	}

	@PostMapping("/issue-slips/create-manual")
	public ResponseEntity<IssueSlipResponse> createManualIssueSlip(@org.springframework.web.bind.annotation.RequestBody com.edu.pcmaster.dto.inventory.CreateManualIssueSlipRequest request) {
		return ResponseEntity.ok(toResponse(inventoryService.createManualIssueSlip(request)));
	}

	@PostMapping("/issue-slips/{id}/dispatch")
	public ResponseEntity<IssueSlipResponse> dispatchIssueSlip(@PathVariable Long id) {
		return ResponseEntity.ok(toResponse(inventoryService.dispatchIssueSlip(id)));
	}

	private IssueSlipResponse toResponse(InventoryIssueSlip slip) {
		return new IssueSlipResponse(
				slip.getId(),
				slip.getCode(),
				slip.getOrder() != null ? slip.getOrder().getId() : null,
				slip.getStatus(),
				slip.getDocumentUrl(),
				slip.getCreatedAt(),
				slip.getCompletedAt(),
				slip.getOrder() != null ? slip.getOrder().getRecipientName() : getVietnameseExportReason(slip.getExportReason()),
				slip.getOrder() != null ? slip.getOrder().getRecipientPhone() : "N/A",
				slip.getOrder() != null ? slip.getOrder().getShippingAddress() : "N/A",
				slip.getOrder() != null ? slip.getOrder().getDeliveryType().name() : "SHOWROOM_PICKUP",
				slip.getExportReason() != null ? getVietnameseExportReason(slip.getExportReason()) : "Xuất hàng bán lẻ (Đơn hàng)",
				slip.getOrder() != null
						? slip.getOrder().getItems().stream()
								.map(item -> new IssueSlipResponse.IssueSlipItemResponse(
										item.getId(),
										item.getProduct() != null ? item.getProduct().getId() : null,
										item.getProduct() != null ? item.getProduct().getName() : "N/A",
										item.getQuantity()
								))
								.collect(Collectors.toList())
						: slip.getItems().stream()
								.map(item -> new IssueSlipResponse.IssueSlipItemResponse(
										item.getId(),
										item.getProduct() != null ? item.getProduct().getId() : null,
										item.getProduct() != null ? item.getProduct().getName() : "N/A",
										item.getQuantity()
								))
								.collect(Collectors.toList())
		);
	}

	private String getVietnameseExportReason(String reason) {
		if (reason == null) return "Xuất hàng bán lẻ";
		switch (reason) {
			case "RETAIL_SALE": return "Xuất hàng bán lẻ";
			case "PROVIDER_RETURN": return "Xuất trả hàng lỗi cho nhà cung cấp";
			case "PC_ASSEMBLY": return "Xuất linh kiện để lắp ráp PC bộ";
			default: return reason;
		}
	}
}

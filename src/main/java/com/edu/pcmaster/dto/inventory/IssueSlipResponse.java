package com.edu.pcmaster.dto.inventory;

import java.time.Instant;
import java.util.List;

public record IssueSlipResponse(
		Long id,
		String code,
		Long orderId,
		String status,
		String documentUrl,
		Instant createdAt,
		Instant completedAt,
		String recipientName,
		String recipientPhone,
		String shippingAddress,
		String deliveryType,
		String exportReason,
		List<IssueSlipItemResponse> items
) {
	public record IssueSlipItemResponse(
			Long id,
			Long productId,
			String productName,
			Integer quantity
	) {
	}
}

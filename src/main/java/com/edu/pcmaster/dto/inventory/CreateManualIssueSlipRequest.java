package com.edu.pcmaster.dto.inventory;

import java.util.List;

public record CreateManualIssueSlipRequest(
		String exportReason,
		Long orderId,
		List<ItemRequest> items
) {
	public record ItemRequest(
			Long productId,
			Integer quantity
	) {
	}
}

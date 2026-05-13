package com.edu.pcmaster.dto.purchase;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record PurchaseOrderRequest(
		@NotNull Long supplierId,
		@NotEmpty @Valid List<PurchaseOrderItemRequest> items
) {
}



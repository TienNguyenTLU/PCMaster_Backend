package com.edu.pcmaster.dto.order;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

public record OrderRequest(
		@NotEmpty @Valid List<OrderItemRequest> items
) {
}



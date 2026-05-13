package com.edu.pcmaster.dto.build;

import java.util.List;

import com.edu.pcmaster.dto.product.ProductResponse;

public record CompatibleComponentsResponse(
		String type,
		List<ProductResponse> products
) {
}


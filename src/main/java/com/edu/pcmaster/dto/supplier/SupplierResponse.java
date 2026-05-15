package com.edu.pcmaster.dto.supplier;

import java.util.Set;

public record SupplierResponse(
		Long id,
		String name,
		String email,
		String phone,
		String address,
		String contactPerson,
		Set<Long> brandIds
) {
}


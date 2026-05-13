package com.edu.pcmaster.dto.supplier;

public record SupplierResponse(
		Long id,
		String name,
		String email,
		String phone,
		String address,
		String contactPerson
) {
}


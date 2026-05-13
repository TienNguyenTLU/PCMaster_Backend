package com.edu.pcmaster.dto.supplier;

import jakarta.validation.constraints.NotBlank;

public record SupplierRequest(
		@NotBlank String name,
		String email,
		String phone,
		String address,
		String contactPerson
) {
}


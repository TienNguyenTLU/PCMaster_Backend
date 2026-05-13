package com.edu.pcmaster.controllers;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.edu.pcmaster.dto.supplier.SupplierRequest;
import com.edu.pcmaster.dto.supplier.SupplierResponse;
import com.edu.pcmaster.models.Supplier;
import com.edu.pcmaster.services.SupplierService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/admin/suppliers")
@PreAuthorize("hasRole('ADMIN')")
public class SupplierController {
	private final SupplierService supplierService;

	public SupplierController(SupplierService supplierService) {
		this.supplierService = supplierService;
	}

	@GetMapping
	public List<SupplierResponse> list() {
		return supplierService.findAll().stream()
				.map(this::toResponse)
				.toList();
	}

	@PostMapping
	public SupplierResponse create(@Valid @RequestBody SupplierRequest request) {
		return toResponse(supplierService.create(request));
	}

	private SupplierResponse toResponse(Supplier supplier) {
		return new SupplierResponse(
				supplier.getId(),
				supplier.getName(),
				supplier.getEmail(),
				supplier.getPhone(),
				supplier.getAddress(),
				supplier.getContactPerson()
		);
	}
}


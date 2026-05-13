package com.edu.pcmaster.services;

import java.util.List;

import org.springframework.stereotype.Service;

import com.edu.pcmaster.common.exception.ResourceNotFoundException;
import com.edu.pcmaster.dto.supplier.SupplierRequest;
import com.edu.pcmaster.models.Supplier;
import com.edu.pcmaster.repositories.SupplierRepository;

@Service
public class SupplierService {
	private final SupplierRepository supplierRepository;

	public SupplierService(SupplierRepository supplierRepository) {
		this.supplierRepository = supplierRepository;
	}

	public List<Supplier> findAll() {
		return supplierRepository.findAll();
	}

	public Supplier getById(Long id) {
		return supplierRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Supplier not found"));
	}

	public Supplier create(SupplierRequest request) {
		Supplier supplier = new Supplier();
		supplier.setName(request.name());
		supplier.setEmail(request.email());
		supplier.setPhone(request.phone());
		supplier.setAddress(request.address());
		supplier.setContactPerson(request.contactPerson());
		return supplierRepository.save(supplier);
	}
}


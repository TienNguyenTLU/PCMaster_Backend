package com.edu.pcmaster.services;

import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.edu.pcmaster.common.exception.ResourceNotFoundException;
import com.edu.pcmaster.dto.supplier.SupplierRequest;
import com.edu.pcmaster.models.Supplier;
import com.edu.pcmaster.repositories.BrandRepository;
import com.edu.pcmaster.repositories.SupplierRepository;

@Service
public class SupplierService {
	private final SupplierRepository supplierRepository;
	private final BrandRepository brandRepository;

	public SupplierService(SupplierRepository supplierRepository, BrandRepository brandRepository) {
		this.supplierRepository = supplierRepository;
		this.brandRepository = brandRepository;
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
		updateFields(supplier, request);
		return supplierRepository.save(supplier);
	}

	public Supplier update(Long id, SupplierRequest request) {
		Supplier supplier = getById(id);
		updateFields(supplier, request);
		return supplierRepository.save(supplier);
	}

	public void delete(Long id) {
		Supplier supplier = getById(id);
		supplierRepository.delete(supplier);
	}

	private void updateFields(Supplier supplier, SupplierRequest request) {
		supplier.setName(request.name());
		supplier.setEmail(request.email());
		supplier.setPhone(request.phone());
		supplier.setAddress(request.address());
		supplier.setContactPerson(request.contactPerson());
		
		if (request.brandIds() != null) {
			var brands = request.brandIds().stream()
					.map(brandId -> brandRepository.findById(brandId)
							.orElseThrow(() -> new ResourceNotFoundException("Brand not found: " + brandId)))
					.collect(Collectors.toSet());
			supplier.setBrands(brands);
		} else {
			supplier.setBrands(new HashSet<>());
		}
	}
}

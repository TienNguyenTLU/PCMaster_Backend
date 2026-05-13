package com.edu.pcmaster.services;

import java.util.List;

import org.springframework.stereotype.Service;

import com.edu.pcmaster.common.exception.ResourceNotFoundException;
import com.edu.pcmaster.dto.brand.BrandRequest;
import com.edu.pcmaster.models.Brand;
import com.edu.pcmaster.repositories.BrandRepository;

@Service
public class BrandService {
	private final BrandRepository brandRepository;

	public BrandService(BrandRepository brandRepository) {
		this.brandRepository = brandRepository;
	}

	public List<Brand> findAll() {
		return brandRepository.findAll();
	}

	public Brand getById(Long id) {
		return brandRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Brand not found"));
	}

	public Brand create(BrandRequest request) {
		Brand brand = new Brand();
		brand.setName(request.name());
		brand.setLogoUrl(request.logoUrl());
		return brandRepository.save(brand);
	}

	public Brand update(Long id, BrandRequest request) {
		Brand brand = getById(id);
		brand.setName(request.name());
		brand.setLogoUrl(request.logoUrl());
		return brandRepository.save(brand);
	}

	public void delete(Long id) {
		Brand brand = getById(id);
		brandRepository.delete(brand);
	}
}

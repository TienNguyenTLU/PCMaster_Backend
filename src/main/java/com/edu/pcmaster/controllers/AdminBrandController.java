package com.edu.pcmaster.controllers;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.edu.pcmaster.dto.brand.BrandRequest;
import com.edu.pcmaster.dto.brand.BrandResponse;
import com.edu.pcmaster.models.Brand;
import com.edu.pcmaster.services.BrandService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/admin/brands")
@PreAuthorize("hasRole('ADMIN')")
public class AdminBrandController {
	private final BrandService brandService;

	public AdminBrandController(BrandService brandService) {
		this.brandService = brandService;
	}

	@GetMapping
	public List<BrandResponse> list() {
		return brandService.findAll().stream()
				.map(this::toResponse)
				.toList();
	}

	@PostMapping
	public BrandResponse create(@Valid @RequestBody BrandRequest request) {
		return toResponse(brandService.create(request));
	}

	@PutMapping("/{id}")
	public BrandResponse update(@PathVariable Long id, @Valid @RequestBody BrandRequest request) {
		return toResponse(brandService.update(id, request));
	}

	@DeleteMapping("/{id}")
	public void delete(@PathVariable Long id) {
		brandService.delete(id);
	}

	private BrandResponse toResponse(Brand brand) {
		return new BrandResponse(brand.getId(), brand.getName(), brand.getLogoUrl());
	}
}

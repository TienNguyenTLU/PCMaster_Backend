package com.edu.pcmaster.controllers;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.edu.pcmaster.services.MediaService;

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
	private final MediaService mediaService;

	public AdminBrandController(BrandService brandService, MediaService mediaService) {
		this.brandService = brandService;
		this.mediaService = mediaService;
	}

	@GetMapping
	public List<BrandResponse> list() {
		return brandService.findAll().stream()
				.map(this::toResponse)
				.toList();
	}

	@PostMapping(consumes = "multipart/form-data")
	public BrandResponse create(
			@RequestParam("name") String name,
			@RequestPart(value = "logo", required = false) org.springframework.web.multipart.MultipartFile logo) {
		String logoUrl = null;
		if (logo != null && !logo.isEmpty()) {
			logoUrl = mediaService.upload(logo, "PCMaster_Storage/Brands");
		}
		return toResponse(brandService.create(new BrandRequest(name, logoUrl)));
	}

	@PutMapping(value = "/{id}", consumes = "multipart/form-data")
	public BrandResponse update(
			@PathVariable Long id,
			@RequestParam("name") String name,
			@RequestPart(value = "logo", required = false) org.springframework.web.multipart.MultipartFile logo) {
		String logoUrl = null; // We might want to keep the old logo if no new one is provided, 
							   // but BrandRequest currently overwrites. 
							   // Let's check BrandService update logic.
		if (logo != null && !logo.isEmpty()) {
			logoUrl = mediaService.upload(logo, "PCMaster_Storage/Brands");
		} else {
			// If no new logo, we should probably keep the existing one.
			// But for simplicity of the "Brands only have name and logo" request, 
			// I'll just use the provided name.
			Brand existing = brandService.getById(id);
			logoUrl = existing.getLogoUrl();
		}
		return toResponse(brandService.update(id, new BrandRequest(name, logoUrl)));
	}

	@DeleteMapping("/{id}")
	public void delete(@PathVariable Long id) {
		brandService.delete(id);
	}

	private BrandResponse toResponse(Brand brand) {
		return new BrandResponse(brand.getId(), brand.getName(), brand.getLogoUrl());
	}
}

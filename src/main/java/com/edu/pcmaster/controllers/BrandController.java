package com.edu.pcmaster.controllers;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.edu.pcmaster.dto.brand.BrandResponse;
import com.edu.pcmaster.models.Brand;
import com.edu.pcmaster.services.BrandService;

@RestController
@RequestMapping("/api/brands")
public class BrandController {
	private final BrandService brandService;

	public BrandController(BrandService brandService) {
		this.brandService = brandService;
	}

	@GetMapping
	public List<BrandResponse> list() {
		return brandService.findAll().stream()
				.map(this::toResponse)
				.toList();
	}

	private BrandResponse toResponse(Brand brand) {
		return new BrandResponse(brand.getId(), brand.getName(), brand.getLogoUrl());
	}
}

package com.edu.pcmaster.controllers;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.edu.pcmaster.dto.brand.BrandResponse;
import com.edu.pcmaster.dto.category.CategoryResponse;
import com.edu.pcmaster.dto.product.ProductRequest;
import com.edu.pcmaster.dto.product.ProductResponse;
import com.edu.pcmaster.models.Product;
import com.edu.pcmaster.services.ProductService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/admin/products")
@PreAuthorize("hasRole('ADMIN')")
public class AdminProductController {
	private final ProductService productService;

	public AdminProductController(ProductService productService) {
		this.productService = productService;
	}

	@PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ProductResponse create(@Valid @RequestPart("data") ProductRequest request,
								@RequestPart(value = "thumbnail", required = false) MultipartFile thumbnail) {
		return toResponse(productService.create(request, thumbnail));
	}

	@PutMapping("/{id}")
	public ProductResponse update(@PathVariable Long id, @Valid @RequestBody ProductRequest request) {
		return toResponse(productService.update(id, request));
	}

	@DeleteMapping("/{id}")
	public void delete(@PathVariable Long id) {
		productService.delete(id);
	}

	private ProductResponse toResponse(Product product) {
		return new ProductResponse(
				product.getId(),
				product.getCategory() == null ? null : product.getCategory().getId(),
				product.getBrand() == null ? null : product.getBrand().getId(),
				toCategoryResponse(product),
				toBrandResponse(product),
				product.getName(),
				product.getSlug(),
				product.getPrice(),
				product.getStock(),
				product.getThumbnailUrl(),
				product.getDescription(),
				product.getSpecsJson() == null ? null : product.getSpecsJson().toString(),
				product.getCreatedAt(),
				product.getUpdatedAt()
		);
	}

	private CategoryResponse toCategoryResponse(Product product) {
		if (product.getCategory() == null) {
			return null;
		}
		return new CategoryResponse(
				product.getCategory().getId(),
				product.getCategory().getName(),
				product.getCategory().getSlug(),
				product.getCategory().getParent() == null ? null : product.getCategory().getParent().getId()
		);
	}

	private BrandResponse toBrandResponse(Product product) {
		if (product.getBrand() == null) {
			return null;
		}
		return new BrandResponse(
				product.getBrand().getId(),
				product.getBrand().getName(),
				product.getBrand().getLogoUrl()
		);
	}
}

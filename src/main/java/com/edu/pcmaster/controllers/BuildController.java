package com.edu.pcmaster.controllers;

import java.util.List;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.edu.pcmaster.dto.build.CompatibleComponentsResponse;
import com.edu.pcmaster.dto.build.PcBuildItemRequest;
import com.edu.pcmaster.dto.build.PcBuildItemResponse;
import com.edu.pcmaster.dto.build.PcBuildRequest;
import com.edu.pcmaster.dto.build.PcBuildResponse;
import com.edu.pcmaster.dto.brand.BrandResponse;
import com.edu.pcmaster.dto.category.CategoryResponse;
import com.edu.pcmaster.dto.product.ProductResponse;
import com.edu.pcmaster.models.ComponentType;
import com.edu.pcmaster.models.PcBuild;
import com.edu.pcmaster.models.Product;
import com.edu.pcmaster.services.BuildService;
import com.edu.pcmaster.services.CurrentUserService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/builds")
public class BuildController {
	private final BuildService buildService;
	private final CurrentUserService currentUserService;

	public BuildController(BuildService buildService, CurrentUserService currentUserService) {
		this.buildService = buildService;
		this.currentUserService = currentUserService;
	}

	@GetMapping
	public List<PcBuildResponse> list() {
		return buildService.findByUser(currentUserService.requireUser()).stream()
				.map(this::toResponse)
				.toList();
	}

	@PostMapping
	public PcBuildResponse create(@Valid @RequestBody PcBuildRequest request) {
		return toResponse(buildService.create(request, currentUserService.requireUser()));
	}

	@GetMapping("/{id}")
	public PcBuildResponse detail(@PathVariable Long id) {
		return toResponse(buildService.getById(id, currentUserService.requireUser()));
	}

	@PostMapping("/{id}/items")
	public PcBuildResponse addItem(@PathVariable Long id, @Valid @RequestBody PcBuildItemRequest request) {
		return toResponse(buildService.addItem(id, request, currentUserService.requireUser()));
	}

	@PutMapping("/{id}/items/{itemId}")
	public PcBuildResponse updateItem(@PathVariable Long id, @PathVariable Long itemId,
								@Valid @RequestBody PcBuildItemRequest request) {
		return toResponse(buildService.updateItem(id, itemId, request, currentUserService.requireUser()));
	}

	@DeleteMapping("/{id}/items/{itemId}")
	public PcBuildResponse deleteItem(@PathVariable Long id, @PathVariable Long itemId) {
		return toResponse(buildService.deleteItem(id, itemId, currentUserService.requireUser()));
	}

	@GetMapping("/{id}/compatible-components")
	public CompatibleComponentsResponse compatibleComponents(@PathVariable Long id,
													@RequestParam ComponentType type) {
		PcBuild build = buildService.getById(id, currentUserService.requireUser());
		List<ProductResponse> products = buildService.findCompatibleComponents(build, type).stream()
				.map(this::toProductResponse)
				.toList();
		return new CompatibleComponentsResponse(type.name(), products);
	}

	private PcBuildResponse toResponse(PcBuild build) {
		return new PcBuildResponse(
				build.getId(),
				build.getUser() == null ? null : build.getUser().getId(),
				build.getName(),
				build.getTotalPrice(),
				build.getTotalPower(),
				build.getCreatedAt(),
				build.getItems().stream()
						.map(item -> new PcBuildItemResponse(
								item.getId(),
								item.getProduct() == null ? null : item.getProduct().getId(),
								item.getComponentType()
						))
						.toList()
		);
	}

	private ProductResponse toProductResponse(Product product) {
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

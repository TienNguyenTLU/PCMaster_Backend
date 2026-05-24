package com.edu.pcmaster.controllers;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.edu.pcmaster.dto.brand.BrandResponse;
import com.edu.pcmaster.dto.category.CategoryResponse;
import com.edu.pcmaster.dto.product.ProductResponse;
import com.edu.pcmaster.models.Product;
import com.edu.pcmaster.services.ProductService;

@RestController
@RequestMapping("/api/products")
public class ProductController {
	private final ProductService productService;

	public ProductController(ProductService productService) {
		this.productService = productService;
	}

	@GetMapping
	public Page<ProductResponse> search(@RequestParam(required = false) Long categoryId,
								@RequestParam(required = false) Long brandId,
								@RequestParam(required = false) String keyword,
								@RequestParam(defaultValue = "0") int page,
								@RequestParam(defaultValue = "10") int size) {
		return productService.search(categoryId, brandId, keyword, page, size)
				.map(this::toResponse);
	}

	@GetMapping("/{id}")
	public ProductResponse detail(@PathVariable Long id) {
		return toResponse(productService.getById(id));
	}

	private ProductResponse toResponse(Product product) {
		List<ProductResponse.PcComponentResponse> pcComponents = null;
		if (product.getPcSystemDetail() != null && product.getPcSystemDetail().getComponents() != null) {
			pcComponents = product.getPcSystemDetail().getComponents().stream()
					.map(comp -> new ProductResponse.PcComponentResponse(
							comp.getComponentProduct().getId(),
							comp.getComponentProduct().getName(),
							comp.getComponentProduct().getThumbnailUrl(),
							comp.getComponentProduct().getPrice(),
							comp.getQuantity()
					))
					.toList();
		}
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
				product.getUpdatedAt(),
				pcComponents
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

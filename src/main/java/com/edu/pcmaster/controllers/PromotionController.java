package com.edu.pcmaster.controllers;

import org.springframework.web.bind.annotation.*;
import com.edu.pcmaster.dto.promotion.*;
import com.edu.pcmaster.dto.product.ProductResponse;
import com.edu.pcmaster.models.*;
import com.edu.pcmaster.repositories.*;
import com.edu.pcmaster.services.ProductService;
import com.edu.pcmaster.common.exception.ResourceNotFoundException;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/promotions")
public class PromotionController {
	private final PromotionRepository promotionRepository;
	private final ProductService productService;
	private final ProductController productController;

	public PromotionController(PromotionRepository promotionRepository, ProductService productService, ProductController productController) {
		this.promotionRepository = promotionRepository;
		this.productService = productService;
		this.productController = productController;
	}

	@GetMapping("/active")
	public List<PromotionResponse> listActive() {
		return promotionRepository.findActivePromotions(Instant.now()).stream()
				.map(this::toResponse)
				.toList();
	}

	@GetMapping("/{slug}")
	public PromotionResponseWithProducts detailBySlug(@PathVariable String slug) {
		Promotion promotion = promotionRepository.findBySlug(slug)
				.orElseThrow(() -> new ResourceNotFoundException("Promotion campaign not found"));
		
		List<ProductResponse> products = promotion.getProducts().stream()
				.map(p -> productController.detail(p.getId()))
				.toList();

		return new PromotionResponseWithProducts(
				promotion.getId(),
				promotion.getName(),
				promotion.getSlug(),
				promotion.getDescription(),
				promotion.getBannerUrl(),
				promotion.getDiscountPercent(),
				promotion.getStartDate(),
				promotion.getEndDate(),
				promotion.getActive(),
				products,
				promotion.getCreatedAt()
		);
	}

	private PromotionResponse toResponse(Promotion p) {
		List<Long> productIds = p.getProducts().stream()
				.map(Product::getId)
				.toList();
		return new PromotionResponse(
				p.getId(),
				p.getName(),
				p.getSlug(),
				p.getDescription(),
				p.getBannerUrl(),
				p.getDiscountPercent(),
				p.getStartDate(),
				p.getEndDate(),
				p.getActive(),
				productIds,
				p.getCreatedAt()
		);
	}

	public record PromotionResponseWithProducts(
			Long id,
			String name,
			String slug,
			String description,
			String bannerUrl,
			Integer discountPercent,
			Instant startDate,
			Instant endDate,
			Boolean active,
			List<ProductResponse> products,
			Instant createdAt
	) {}
}

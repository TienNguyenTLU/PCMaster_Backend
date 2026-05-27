package com.edu.pcmaster.controllers;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import com.edu.pcmaster.dto.promotion.*;
import com.edu.pcmaster.models.*;
import com.edu.pcmaster.repositories.*;
import com.edu.pcmaster.common.exception.ResourceNotFoundException;
import jakarta.validation.Valid;
import java.util.List;
import com.edu.pcmaster.services.BannerService;
import com.edu.pcmaster.services.MediaService;

@RestController
@RequestMapping("/api/admin/promotions")
@PreAuthorize("hasRole('ADMIN')")
public class AdminPromotionController {
	private final PromotionRepository promotionRepository;
	private final ProductRepository productRepository;
	private final BannerRepository bannerRepository;
	private final BannerService bannerService;
	private final MediaService mediaService;

	public AdminPromotionController(PromotionRepository promotionRepository, ProductRepository productRepository, BannerRepository bannerRepository, BannerService bannerService, MediaService mediaService) {
		this.promotionRepository = promotionRepository;
		this.productRepository = productRepository;
		this.bannerRepository = bannerRepository;
		this.bannerService = bannerService;
		this.mediaService = mediaService;
	}

	@GetMapping
	public List<PromotionResponse> listAll() {
		return promotionRepository.findAll().stream()
				.map(this::toResponse)
				.toList();
	}

	@GetMapping("/{id}")
	public PromotionResponse detail(@PathVariable Long id) {
		Promotion promotion = promotionRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Promotion not found"));
		return toResponse(promotion);
	}

	@PostMapping
	public PromotionResponse create(@Valid @RequestBody PromotionRequest request) {
		Promotion promotion = new Promotion();
		promotion.setName(request.name());
		promotion.setSlug(request.slug());
		promotion.setDescription(request.description());
		promotion.setBannerUrl(request.bannerUrl());
		promotion.setDiscountPercent(request.discountPercent());
		promotion.setStartDate(request.startDate());
		promotion.setEndDate(request.endDate());
		if (request.active() != null) {
			promotion.setActive(request.active());
		}
		if (request.productIds() != null && !request.productIds().isEmpty()) {
			List<Product> products = productRepository.findAllById(request.productIds());
			promotion.setProducts(products);
		}
		Promotion savedPromotion = promotionRepository.save(promotion);

		// Auto create banner
		if (savedPromotion.getBannerUrl() != null && !savedPromotion.getBannerUrl().isEmpty()) {
			int maxOrder = bannerRepository.findAll().stream()
					.mapToInt(Banner::getDisplayOrder)
					.max()
					.orElse(0);
			Banner banner = new Banner();
			banner.setImageUrl(savedPromotion.getBannerUrl());
			banner.setLinkUrl("/promotions/" + savedPromotion.getSlug());
			banner.setDisplayOrder(maxOrder + 1);
			bannerRepository.save(banner);
		}

		return toResponse(savedPromotion);
	}

	@PutMapping("/{id}")
	public PromotionResponse update(@PathVariable Long id, @Valid @RequestBody PromotionRequest request) {
		Promotion promotion = promotionRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Promotion not found"));
		String oldSlug = promotion.getSlug();
		promotion.setName(request.name());
		promotion.setSlug(request.slug());
		promotion.setDescription(request.description());
		promotion.setBannerUrl(request.bannerUrl());
		promotion.setDiscountPercent(request.discountPercent());
		promotion.setStartDate(request.startDate());
		promotion.setEndDate(request.endDate());
		if (request.active() != null) {
			promotion.setActive(request.active());
		}
		
		promotion.getProducts().clear();
		if (request.productIds() != null && !request.productIds().isEmpty()) {
			List<Product> products = productRepository.findAllById(request.productIds());
			promotion.setProducts(products);
		}
		
		Promotion savedPromotion = promotionRepository.save(promotion);

		// Update or auto-create banner
		String oldLink = "/promotions/" + oldSlug;
		String newLink = "/promotions/" + savedPromotion.getSlug();

		List<Banner> existingBanners = bannerRepository.findAll().stream()
				.filter(b -> oldLink.equals(b.getLinkUrl()))
				.toList();

		if (!existingBanners.isEmpty()) {
			for (Banner b : existingBanners) {
				b.setLinkUrl(newLink);
				b.setImageUrl(savedPromotion.getBannerUrl());
				bannerRepository.save(b);
			}
		} else if (savedPromotion.getBannerUrl() != null && !savedPromotion.getBannerUrl().isEmpty()) {
			int maxOrder = bannerRepository.findAll().stream()
					.mapToInt(Banner::getDisplayOrder)
					.max()
					.orElse(0);
			Banner banner = new Banner();
			banner.setImageUrl(savedPromotion.getBannerUrl());
			banner.setLinkUrl(newLink);
			banner.setDisplayOrder(maxOrder + 1);
			bannerRepository.save(banner);
		}

		return toResponse(savedPromotion);
	}

	@DeleteMapping("/{id}")
	public void delete(@PathVariable Long id) {
		Promotion promotion = promotionRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Promotion not found"));
		
		// Delete any corresponding banners using bannerService to clean up cloud images
		String promotionLink = "/promotions/" + promotion.getSlug();
		bannerRepository.findAll().stream()
				.filter(b -> promotionLink.equals(b.getLinkUrl()))
				.forEach(b -> {
					try {
						bannerService.deleteBanner(b.getId());
					} catch (Exception e) {
						System.err.println("Failed to delete banner: " + e.getMessage());
					}
				});

		// Delete promotion's own banner image from Cloudinary
		String promoBannerUrl = promotion.getBannerUrl();
		if (promoBannerUrl != null && promoBannerUrl.contains("cloudinary.com")) {
			try {
				String publicId = extractPublicIdFromUrl(promoBannerUrl);
				mediaService.delete(publicId);
			} catch (Exception e) {
				System.err.println("Failed to delete promotion banner image from Cloudinary: " + e.getMessage());
			}
		}

		promotionRepository.delete(promotion);
	}

	private String extractPublicIdFromUrl(String url) {
		int uploadIndex = url.indexOf("/upload/");
		if (uploadIndex == -1) {
			throw new IllegalArgumentException("Invalid Cloudinary URL");
		}
		int versionIndex = url.indexOf("/v", uploadIndex + 8);
		if (versionIndex == -1) {
			throw new IllegalArgumentException("Invalid Cloudinary URL: Missing version");
		}
		int startIndex = url.indexOf('/', versionIndex + 1) + 1;
		int endIndex = url.lastIndexOf('.');
		if (startIndex == -1 || endIndex == -1 || startIndex >= endIndex) {
			throw new IllegalArgumentException("Invalid Cloudinary URL: Cannot extract public ID");
		}
		return url.substring(startIndex, endIndex);
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
}

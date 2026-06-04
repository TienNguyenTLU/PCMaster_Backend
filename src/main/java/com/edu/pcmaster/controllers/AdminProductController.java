package com.edu.pcmaster.controllers;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;
import java.util.Map;
import java.math.BigDecimal;

import com.edu.pcmaster.dto.brand.BrandResponse;
import com.edu.pcmaster.dto.category.CategoryResponse;
import com.edu.pcmaster.dto.product.GearvnImportRequest;
import com.edu.pcmaster.dto.product.ProductRequest;
import com.edu.pcmaster.dto.product.ProductResponse;
import com.edu.pcmaster.models.Product;
import com.edu.pcmaster.services.ProductService;
import com.edu.pcmaster.services.GearvnCrawlerService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/admin/products")
@PreAuthorize("hasRole('ADMIN')")
public class AdminProductController {
	private final ProductService productService;
	private final GearvnCrawlerService gearvnCrawlerService;

	public AdminProductController(ProductService productService, GearvnCrawlerService gearvnCrawlerService) {
		this.productService = productService;
		this.gearvnCrawlerService = gearvnCrawlerService;
	}

	@PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ProductResponse create(@Valid @RequestPart("data") ProductRequest request,
								@RequestPart(value = "thumbnail", required = false) MultipartFile thumbnail) {
		Map<Long, Integer> discountsMap = productService.getActiveProductDiscountsMap();
		return toResponse(productService.create(request, thumbnail), discountsMap);
	}

	@PutMapping("/{id}")
	public ProductResponse update(@PathVariable Long id, @Valid @RequestBody ProductRequest request) {
		Map<Long, Integer> discountsMap = productService.getActiveProductDiscountsMap();
		return toResponse(productService.update(id, request), discountsMap);
	}

	@DeleteMapping("/{id}")
	public void delete(@PathVariable Long id) {
		productService.delete(id);
	}

	@PostMapping("/gearvn/preview")
	public Map<String, Object> previewGearvnProduct(@Valid @RequestBody GearvnImportRequest request) {
		return gearvnCrawlerService.previewProduct(request);
	}

	@PostMapping("/gearvn/import")
	public ProductResponse importFromGearvn(@Valid @RequestBody GearvnImportRequest request) {
		return gearvnCrawlerService.importProduct(request);
	}



	private ProductResponse toResponse(Product product, Map<Long, Integer> discountsMap) {
		List<ProductResponse.PcComponentResponse> pcComponents = null;
		if (product.getPcSystemDetail() != null && product.getPcSystemDetail().getComponents() != null) {
			pcComponents = product.getPcSystemDetail().getComponents().stream()
					.map(comp -> {
						Product componentProduct = comp.getComponentProduct();
						Integer compDiscountPercent = discountsMap.get(componentProduct.getId());
						BigDecimal compDiscountPrice = productService.calculateDiscountPrice(componentProduct.getPrice(), compDiscountPercent);
						return new ProductResponse.PcComponentResponse(
								componentProduct.getId(),
								componentProduct.getName(),
								componentProduct.getThumbnailUrl(),
								compDiscountPrice != null ? compDiscountPrice : componentProduct.getPrice(),
								comp.getQuantity()
						);
					})
					.toList();
		}
		
		Integer discountPercent = discountsMap.get(product.getId());
		BigDecimal discountPrice = productService.calculateDiscountPrice(product.getPrice(), discountPercent);

		return new ProductResponse(
				product.getId(),
				product.getCategory() == null ? null : product.getCategory().getId(),
				product.getBrand() == null ? null : product.getBrand().getId(),
				toCategoryResponse(product),
				toBrandResponse(product),
				product.getName(),
				product.getSlug(),
				product.getPrice(),
				discountPrice,
				discountPercent,
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

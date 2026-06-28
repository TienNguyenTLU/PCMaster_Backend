package com.edu.pcmaster.services;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.edu.pcmaster.common.exception.BadRequestException;
import com.edu.pcmaster.common.exception.ResourceNotFoundException;
import com.edu.pcmaster.dto.product.ProductRequest;
import com.edu.pcmaster.models.Brand;
import com.edu.pcmaster.models.Category;
import com.edu.pcmaster.models.PcSystemComponent;
import com.edu.pcmaster.models.Product;
import com.edu.pcmaster.models.ProductImage;
import org.springframework.transaction.annotation.Transactional;
import com.edu.pcmaster.models.InventoryBatch;
import com.edu.pcmaster.repositories.BrandRepository;
import com.edu.pcmaster.repositories.CategoryRepository;
import com.edu.pcmaster.repositories.InventoryBatchRepository;
import com.edu.pcmaster.repositories.ProductRepository;
import com.edu.pcmaster.repositories.PromotionRepository;
import java.time.Instant;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import java.util.HashMap;

@Service
public class ProductService {
	private final ProductRepository productRepository;
	private final CategoryRepository categoryRepository;
	private final BrandRepository brandRepository;
	private final MediaService mediaService;
	private final ObjectMapper objectMapper;
	private final InventoryService inventoryService;
	private final InventoryBatchRepository inventoryBatchRepository;
	private final PromotionRepository promotionRepository;

	public ProductService(ProductRepository productRepository,
						 CategoryRepository categoryRepository,
						 BrandRepository brandRepository,
						 MediaService mediaService,
						 ObjectMapper objectMapper,
						 InventoryService inventoryService,
						 InventoryBatchRepository inventoryBatchRepository,
						 PromotionRepository promotionRepository) {
		this.productRepository = productRepository;
		this.categoryRepository = categoryRepository;
		this.brandRepository = brandRepository;
		this.mediaService = mediaService;
		this.objectMapper = objectMapper;
		this.inventoryService = inventoryService;
		this.inventoryBatchRepository = inventoryBatchRepository;
		this.promotionRepository = promotionRepository;
	}

	public Page<Product> search(Long categoryId, Long brandId, String keyword, int page, int size) {
		return productRepository.search(categoryId, brandId, keyword, PageRequest.of(page, size));
	}

	public Product getById(Long id) {
		return productRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Product not found"));
	}

	public Product getBySlugOrId(String identifier) {
		if (identifier != null && identifier.matches("^\\d+$")) {
			return productRepository.findById(Long.parseLong(identifier))
					.orElseThrow(() -> new ResourceNotFoundException("Product not found"));
		}
		return productRepository.findBySlug(identifier)
				.orElseThrow(() -> new ResourceNotFoundException("Product not found"));
	}

	public Product create(ProductRequest request, MultipartFile thumbnailFile) {
		Category category = categoryRepository.findById(request.categoryId())
				.orElseThrow(() -> new ResourceNotFoundException("Category not found"));
		Brand brand = brandRepository.findById(request.brandId())
				.orElseThrow(() -> new ResourceNotFoundException("Brand not found"));
		String thumbnailUrl = request.thumbnailUrl();
		if (thumbnailFile != null && !thumbnailFile.isEmpty()) {
			String categorySlug = category.getSlug() != null ? category.getSlug() : "other";
			String folder = String.format("PCMAster_Storage/Product_thumbnails/%s", categorySlug);
			thumbnailUrl = mediaService.upload(thumbnailFile, folder);
		}

		Product product = new Product();
		product.setCategory(category);
		product.setBrand(brand);
		product.setName(request.name());
		product.setSlug(request.slug());
		product.setPrice(request.price());
		product.setThumbnailUrl(thumbnailUrl);
		product.setDescription(request.description());
		product.setSpecsJson(parseSpecsJson(request.specsJson()));

		
		if ("PC_SYSTEM".equalsIgnoreCase(category.getSlug().replace("-", "_"))) {
			int pcStock = request.stock() != null ? request.stock() : 0;
			if (request.pcComponents() == null || request.pcComponents().isEmpty()) {
				if (pcStock > 0) {
					throw new BadRequestException("Cấu hình PC lắp sẵn cần ít nhất 1 linh kiện để lắp ráp.");
				}
			} else {
				
				if (pcStock > 0) {
					for (ProductRequest.PcComponentRequest compReq : request.pcComponents()) {
						Product componentProduct = productRepository.findById(compReq.componentProductId())
								.orElseThrow(() -> new ResourceNotFoundException("Component product not found: " + compReq.componentProductId()));
						int totalNeeded = pcStock * compReq.quantity();
						if (componentProduct.getStock() < totalNeeded) {
							throw new BadRequestException("Không đủ số lượng tồn kho cho linh kiện: " + componentProduct.getName() 
									+ " (cần " + totalNeeded + ", hiện có " + componentProduct.getStock() + ")");
						}
					}
				}

				
				BigDecimal totalComponentCost = BigDecimal.ZERO;
				if (pcStock > 0) {
					for (ProductRequest.PcComponentRequest compReq : request.pcComponents()) {
						Product componentProduct = productRepository.findById(compReq.componentProductId())
								.orElseThrow(() -> new ResourceNotFoundException("Component product not found: " + compReq.componentProductId()));
						int totalNeeded = pcStock * compReq.quantity();
						BigDecimal cost = inventoryService.deductStockFIFO(componentProduct, totalNeeded);
						totalComponentCost = totalComponentCost.add(cost);
					}
				}

					for (ProductRequest.PcComponentRequest compReq : request.pcComponents()) {
						Product componentProduct = productRepository.findById(compReq.componentProductId())
								.orElseThrow(() -> new ResourceNotFoundException("Component product not found: " + compReq.componentProductId()));
						
						PcSystemComponent component = new PcSystemComponent();
						component.setProduct(product);
						component.setComponentProduct(componentProduct);
						component.setQuantity(compReq.quantity());
						product.getPcComponents().add(component);
					}
					product.setStock(pcStock);

				
				product = productRepository.save(product);

				
				if (pcStock > 0) {
					InventoryBatch batch = new InventoryBatch();
					batch.setProduct(product);
					batch.setQuantity(pcStock);
					batch.setRemainingQuantity(pcStock);
					BigDecimal costPerPc = totalComponentCost.divide(BigDecimal.valueOf(pcStock), 2, RoundingMode.HALF_UP);
					batch.setImportPrice(costPerPc);
					batch.setImportedAt(Instant.now());
					inventoryBatchRepository.save(batch);
				}
				return product;
			}
		}

		return productRepository.save(product);
	}

	public Product update(Long id, ProductRequest request) {
		Product product = getById(id);
		Category category = categoryRepository.findById(request.categoryId())
				.orElseThrow(() -> new ResourceNotFoundException("Category not found"));
		Brand brand = brandRepository.findById(request.brandId())
				.orElseThrow(() -> new ResourceNotFoundException("Brand not found"));

		product.setCategory(category);
		product.setBrand(brand);
		product.setName(request.name());
		product.setSlug(request.slug());
		product.setPrice(request.price());
		
		
		String oldThumbnailUrl = product.getThumbnailUrl();
		String newThumbnailUrl = request.thumbnailUrl();
		if (oldThumbnailUrl != null && !oldThumbnailUrl.equals(newThumbnailUrl) && oldThumbnailUrl.contains("cloudinary.com")) {
			try {
				String publicId = extractPublicIdFromUrl(oldThumbnailUrl);
				mediaService.delete(publicId);
			} catch (Exception e) {
				System.err.println("Failed to delete old thumbnail from Cloudinary: " + e.getMessage());
			}
		}

		product.setThumbnailUrl(newThumbnailUrl);
		product.setDescription(request.description());
		product.setSpecsJson(parseSpecsJson(request.specsJson()));

		
		if ("PC_SYSTEM".equalsIgnoreCase(category.getSlug().replace("-", "_"))) {
			if (request.pcComponents() != null) {
				product.getPcComponents().clear();

				for (ProductRequest.PcComponentRequest compReq : request.pcComponents()) {
					Product componentProduct = productRepository.findById(compReq.componentProductId())
							.orElseThrow(() -> new ResourceNotFoundException("Component product not found: " + compReq.componentProductId()));
					
					PcSystemComponent component = new PcSystemComponent();
					component.setProduct(product);
					component.setComponentProduct(componentProduct);
					component.setQuantity(compReq.quantity());
					product.getPcComponents().add(component);
				}
			}
		} else {
			product.getPcComponents().clear();
		}

		return productRepository.save(product);
	}

	private JsonNode parseSpecsJson(String specsJson) {
		if (specsJson == null || specsJson.isBlank()) {
			return null;
		}
		try {
			JsonNode parsed = objectMapper.readTree(specsJson);
			String componentType = "";
			if (parsed.has("component_type")) {
				componentType = parsed.get("component_type").asText();
			}
			return com.edu.pcmaster.common.util.ProductSpecNormalizer.normalize(parsed, componentType);
		} catch (Exception ex) {
			throw new BadRequestException("Invalid specsJson format");
		}
	}

	public Map<Long, Integer> getActiveProductDiscountsMap() {
		List<Object[]> list = promotionRepository.findActiveProductDiscounts(Instant.now());
		Map<Long, Integer> map = new HashMap<>();
		for (Object[] row : list) {
			Long productId = (Long) row[0];
			Integer discountPercent = (Integer) row[1];
			map.merge(productId, discountPercent, Math::max);
		}
		return map;
	}

	public BigDecimal calculateDiscountPrice(BigDecimal originalPrice, Integer discountPercent) {
		if (discountPercent == null || discountPercent <= 0) {
			return null;
		}
		BigDecimal discount = originalPrice
				.multiply(BigDecimal.valueOf(discountPercent))
				.divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
		return originalPrice.subtract(discount);
	}

	@Transactional
	public void delete(Long id) {
		Product product = getById(id);
		String thumbnailUrl = product.getThumbnailUrl();
		if (thumbnailUrl != null && thumbnailUrl.contains("cloudinary.com")) {
			try {
				String publicId = extractPublicIdFromUrl(thumbnailUrl);
				mediaService.delete(publicId);
			} catch (Exception e) {
				System.err.println("Failed to delete product thumbnail from Cloudinary: " + e.getMessage());
			}
		}
		
		// Xóa các hình ảnh phụ liên quan trên Cloudinary
		if (product.getImages() != null) {
			for (ProductImage img : product.getImages()) {
				String imgUrl = img.getUrl();
				if (imgUrl != null && imgUrl.contains("cloudinary.com")) {
					try {
						String publicId = extractPublicIdFromUrl(imgUrl);
						mediaService.delete(publicId);
					} catch (Exception e) {
						System.err.println("Failed to delete product image from Cloudinary: " + e.getMessage());
					}
				}
			}
		}
		
		productRepository.delete(product);
	}

	private String extractPublicIdFromUrl(String url) {
		int uploadIndex = url.indexOf("/upload/");
		if (uploadIndex == -1) {
			throw new IllegalArgumentException("Invalid Cloudinary URL");
		}
		
		String path = url.substring(uploadIndex + 8);
		int lastDot = path.lastIndexOf('.');
		if (lastDot != -1) {
			path = path.substring(0, lastDot);
		}
		
		String[] segments = path.split("/");
		StringBuilder publicIdBuilder = new StringBuilder();
		boolean foundPublicIdStart = false;
		
		for (String segment : segments) {
			if (segment.isEmpty()) {
				continue;
			}
			if (!foundPublicIdStart) {
				if (segment.matches("v\\d+")) {
					foundPublicIdStart = true;
					continue;
				}
				if (isTransformation(segment)) {
					continue;
				}
				foundPublicIdStart = true;
			}
			
			if (foundPublicIdStart) {
				if (publicIdBuilder.length() > 0) {
					publicIdBuilder.append("/");
				}
				publicIdBuilder.append(segment);
			}
		}
		
		if (publicIdBuilder.length() == 0) {
			throw new IllegalArgumentException("Invalid Cloudinary URL: Cannot extract public ID");
		}
		
		return publicIdBuilder.toString();
	}

	private boolean isTransformation(String segment) {
		String regex = "^(?:(c|dpr|e|f|fl|g|h|l|p|q|r|t|u|w|x|y|z|ac|br|co|dl|dn|du|eo|fps|ki|so|vc|vs|b|o|a|d|cs)_[a-zA-Z0-9-._]+)(?:,(?:(c|dpr|e|f|fl|g|h|l|p|q|r|t|u|w|x|y|z|ac|br|co|dl|dn|du|eo|fps|ki|so|vc|vs|b|o|a|d|cs)_[a-zA-Z0-9-._]+))*$";
		return segment.matches(regex);
	}
}

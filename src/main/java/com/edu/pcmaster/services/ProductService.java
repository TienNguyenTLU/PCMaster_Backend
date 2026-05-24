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
import com.edu.pcmaster.models.PcSystemDetail;
import com.edu.pcmaster.models.Product;
import com.edu.pcmaster.models.InventoryBatch;
import com.edu.pcmaster.repositories.BrandRepository;
import com.edu.pcmaster.repositories.CategoryRepository;
import com.edu.pcmaster.repositories.InventoryBatchRepository;
import com.edu.pcmaster.repositories.ProductRepository;
import java.time.Instant;
import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class ProductService {
	private final ProductRepository productRepository;
	private final CategoryRepository categoryRepository;
	private final BrandRepository brandRepository;
	private final MediaService mediaService;
	private final ObjectMapper objectMapper;
	private final InventoryService inventoryService;
	private final InventoryBatchRepository inventoryBatchRepository;

	public ProductService(ProductRepository productRepository,
						 CategoryRepository categoryRepository,
						 BrandRepository brandRepository,
						 MediaService mediaService,
						 ObjectMapper objectMapper,
						 InventoryService inventoryService,
						 InventoryBatchRepository inventoryBatchRepository) {
		this.productRepository = productRepository;
		this.categoryRepository = categoryRepository;
		this.brandRepository = brandRepository;
		this.mediaService = mediaService;
		this.objectMapper = objectMapper;
		this.inventoryService = inventoryService;
		this.inventoryBatchRepository = inventoryBatchRepository;
	}

	public Page<Product> search(Long categoryId, Long brandId, String keyword, int page, int size) {
		return productRepository.search(categoryId, brandId, keyword, PageRequest.of(page, size));
	}

	public Product getById(Long id) {
		return productRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Product not found"));
	}

	public Product create(ProductRequest request, MultipartFile thumbnailFile) {
		Category category = categoryRepository.findById(request.categoryId())
				.orElseThrow(() -> new ResourceNotFoundException("Category not found"));
		Brand brand = brandRepository.findById(request.brandId())
				.orElseThrow(() -> new ResourceNotFoundException("Brand not found"));
		String thumbnailUrl = request.thumbnailUrl();
		if (thumbnailFile != null && !thumbnailFile.isEmpty()) {
			thumbnailUrl = mediaService.upload(thumbnailFile);
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

		// Handle PC System components
		if ("PC_SYSTEM".equalsIgnoreCase(category.getSlug().replace("-", "_"))) {
			int pcStock = request.stock() != null ? request.stock() : 0;
			if (request.pcComponents() == null || request.pcComponents().isEmpty()) {
				if (pcStock > 0) {
					throw new BadRequestException("Cấu hình PC lắp sẵn cần ít nhất 1 linh kiện để lắp ráp.");
				}
			} else {
				// Validate component stocks first
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

				// Deduct stock and calculate total component cost
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

				PcSystemDetail pcSystemDetail = new PcSystemDetail();
				pcSystemDetail.setProduct(product);
				List<PcSystemComponent> components = new ArrayList<>();

				for (ProductRequest.PcComponentRequest compReq : request.pcComponents()) {
					Product componentProduct = productRepository.findById(compReq.componentProductId())
							.orElseThrow(() -> new ResourceNotFoundException("Component product not found: " + compReq.componentProductId()));
					
					PcSystemComponent component = new PcSystemComponent();
					component.setPcSystemDetail(pcSystemDetail);
					component.setComponentProduct(componentProduct);
					component.setQuantity(compReq.quantity());
					components.add(component);
				}
				pcSystemDetail.setComponents(components);
				product.setPcSystemDetail(pcSystemDetail);
				product.setStock(pcStock);

				// Save product to get ID
				product = productRepository.save(product);

				// Create inventory batch for the pre-built PC
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
		product.setThumbnailUrl(request.thumbnailUrl());
		product.setDescription(request.description());
		product.setSpecsJson(parseSpecsJson(request.specsJson()));

		// Handle PC System components update
		if ("PC_SYSTEM".equalsIgnoreCase(category.getSlug().replace("-", "_"))) {
			if (request.pcComponents() != null) {
				PcSystemDetail pcSystemDetail = product.getPcSystemDetail();
				if (pcSystemDetail == null) {
					pcSystemDetail = new PcSystemDetail();
					pcSystemDetail.setProduct(product);
					product.setPcSystemDetail(pcSystemDetail);
				}

				// Clear existing components
				pcSystemDetail.getComponents().clear();

				for (ProductRequest.PcComponentRequest compReq : request.pcComponents()) {
					Product componentProduct = productRepository.findById(compReq.componentProductId())
							.orElseThrow(() -> new ResourceNotFoundException("Component product not found: " + compReq.componentProductId()));
					
					PcSystemComponent component = new PcSystemComponent();
					component.setPcSystemDetail(pcSystemDetail);
					component.setComponentProduct(componentProduct);
					component.setQuantity(compReq.quantity());
					pcSystemDetail.getComponents().add(component);
				}
			}
		} else {
			// If category changed from PC_SYSTEM to something else, remove details
			if (product.getPcSystemDetail() != null) {
				product.getPcSystemDetail().getComponents().clear();
				product.setPcSystemDetail(null);
			}
		}

		return productRepository.save(product);
	}

	private JsonNode parseSpecsJson(String specsJson) {
		if (specsJson == null || specsJson.isBlank()) {
			return null;
		}
		try {
			return objectMapper.readTree(specsJson);
		} catch (Exception ex) {
			throw new BadRequestException("Invalid specsJson format");
		}
	}

	public void delete(Long id) {
		Product product = getById(id);
		productRepository.delete(product);
	}
}

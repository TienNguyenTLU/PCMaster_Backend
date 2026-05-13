package com.edu.pcmaster.services;

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
import com.edu.pcmaster.models.Product;
import com.edu.pcmaster.repositories.BrandRepository;
import com.edu.pcmaster.repositories.CategoryRepository;
import com.edu.pcmaster.repositories.ProductRepository;

@Service
public class ProductService {
	private final ProductRepository productRepository;
	private final CategoryRepository categoryRepository;
	private final BrandRepository brandRepository;
	private final MediaService mediaService;
	private final ObjectMapper objectMapper;

	public ProductService(ProductRepository productRepository,
						 CategoryRepository categoryRepository,
						 BrandRepository brandRepository,
						 MediaService mediaService,
						 ObjectMapper objectMapper) {
		this.productRepository = productRepository;
		this.categoryRepository = categoryRepository;
		this.brandRepository = brandRepository;
		this.mediaService = mediaService;
		this.objectMapper = objectMapper;
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


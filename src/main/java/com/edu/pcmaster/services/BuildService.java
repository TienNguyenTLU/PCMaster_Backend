package com.edu.pcmaster.services;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.edu.pcmaster.common.exception.BadRequestException;
import com.edu.pcmaster.common.exception.ResourceNotFoundException;
import com.edu.pcmaster.dto.build.PcBuildItemRequest;
import com.edu.pcmaster.dto.build.PcBuildRequest;
import com.edu.pcmaster.models.ComponentType;
import com.edu.pcmaster.models.PcBuild;
import com.edu.pcmaster.models.PcBuildItem;
import com.edu.pcmaster.models.Product;
import com.edu.pcmaster.models.User;
import com.edu.pcmaster.repositories.PcBuildItemRepository;
import com.edu.pcmaster.repositories.PcBuildRepository;
import com.edu.pcmaster.repositories.ProductRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class BuildService {
	private final PcBuildRepository pcBuildRepository;
	private final PcBuildItemRepository pcBuildItemRepository;
	private final ProductRepository productRepository;
	private final ObjectMapper objectMapper;

	public BuildService(PcBuildRepository pcBuildRepository,
						PcBuildItemRepository pcBuildItemRepository,
						ProductRepository productRepository,
						ObjectMapper objectMapper) {
		this.pcBuildRepository = pcBuildRepository;
		this.pcBuildItemRepository = pcBuildItemRepository;
		this.productRepository = productRepository;
		this.objectMapper = objectMapper;
	}

	public List<PcBuild> findByUser(User user) {
		return pcBuildRepository.findByUser(user);
	}

	public PcBuild getById(Long id, User user) {
		PcBuild build = pcBuildRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Build not found"));
		if (!build.getUser().getId().equals(user.getId())) {
			throw new BadRequestException("Access denied");
		}
		return build;
	}

	public PcBuild create(PcBuildRequest request, User user) {
		PcBuild build = new PcBuild();
		build.setUser(user);
		build.setName(request.name());
		build.setTotalPrice(BigDecimal.ZERO);
		build.setTotalPower(0);
		return pcBuildRepository.save(build);
	}

	@Transactional
	public PcBuild addItem(Long buildId, PcBuildItemRequest request, User user) {
		PcBuild build = getById(buildId, user);
		Product product = productRepository.findById(request.productId())
				.orElseThrow(() -> new ResourceNotFoundException("Product not found"));

		Optional<PcBuildItem> existing = build.getItems().stream()
				.filter(item -> item.getComponentType() == request.componentType())
				.findFirst();

		if (existing.isPresent()) {
			existing.get().setProduct(product);
			pcBuildItemRepository.save(existing.get());
		} else {
			PcBuildItem item = new PcBuildItem();
			item.setPcBuild(build);
			item.setProduct(product);
			item.setComponentType(request.componentType());
			build.getItems().add(item);
			pcBuildItemRepository.save(item);
		}

		recalculateTotals(build);
		return pcBuildRepository.save(build);
	}

	@Transactional
	public PcBuild updateItem(Long buildId, Long itemId, PcBuildItemRequest request, User user) {
		PcBuild build = getById(buildId, user);
		PcBuildItem item = pcBuildItemRepository.findById(itemId)
				.orElseThrow(() -> new ResourceNotFoundException("Build item not found"));
		if (!item.getPcBuild().getId().equals(build.getId())) {
			throw new BadRequestException("Item does not belong to build");
		}

		Product product = productRepository.findById(request.productId())
				.orElseThrow(() -> new ResourceNotFoundException("Product not found"));

		item.setProduct(product);
		item.setComponentType(request.componentType());
		pcBuildItemRepository.save(item);
		recalculateTotals(build);
		return pcBuildRepository.save(build);
	}

	@Transactional
	public PcBuild deleteItem(Long buildId, Long itemId, User user) {
		PcBuild build = getById(buildId, user);
		PcBuildItem item = pcBuildItemRepository.findById(itemId)
				.orElseThrow(() -> new ResourceNotFoundException("Build item not found"));
		if (!item.getPcBuild().getId().equals(build.getId())) {
			throw new BadRequestException("Item does not belong to build");
		}

		build.getItems().remove(item);
		pcBuildItemRepository.delete(item);
		recalculateTotals(build);
		return pcBuildRepository.save(build);
	}

	@Transactional
	public void deleteBuild(Long id, User user) {
		PcBuild build = getById(id, user);
		pcBuildRepository.delete(build);
	}

	public List<Product> findCompatibleComponents(PcBuild build, ComponentType type) {
		String socket = null;
		String ramType = null;

		if (type == ComponentType.MAINBOARD || type == ComponentType.RAM) {
			Product cpu = build.getItems().stream()
					.filter(item -> item.getComponentType() == ComponentType.CPU)
					.map(PcBuildItem::getProduct)
					.findFirst()
					.orElse(null);
			if (cpu != null) {
				socket = getSpecValue(cpu, "socket");
				ramType = getSpecValue(cpu, "ram_type");
			}
		}

		return productRepository.findCompatibleComponents(type.name(), socket, ramType);
	}

	private void recalculateTotals(PcBuild build) {
		BigDecimal totalPrice = BigDecimal.ZERO;
		int totalPower = 0;
		for (PcBuildItem item : build.getItems()) {
			Product product = item.getProduct();
			totalPrice = totalPrice.add(product.getPrice());
			totalPower += getSpecInt(product, "tdp");
		}
		build.setTotalPrice(totalPrice);
		build.setTotalPower(totalPower);
	}

	private String getSpecValue(Product product, String key) {
		Map<String, Object> specs = parseSpecs(product.getSpecsJson());
		Object value = specs.get(key);
		return value == null ? null : value.toString();
	}

	private int getSpecInt(Product product, String key) {
		Map<String, Object> specs = parseSpecs(product.getSpecsJson());
		Object value = specs.get(key);
		if (value instanceof Number number) {
			return number.intValue();
		}
		if (value != null) {
			try {
				return Integer.parseInt(value.toString());
			} catch (NumberFormatException ignored) {
				return 0;
			}
		}
		return 0;
	}

	private Map<String, Object> parseSpecs(com.fasterxml.jackson.databind.JsonNode specsJson) {
		if (specsJson == null || specsJson.isEmpty()) {
			return Map.of();
		}
		try {
			return objectMapper.convertValue(specsJson, new TypeReference<>() {});
		} catch (Exception ex) {
			return Map.of();
		}
	}
}


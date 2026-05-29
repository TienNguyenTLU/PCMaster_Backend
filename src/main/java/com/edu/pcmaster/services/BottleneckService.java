package com.edu.pcmaster.services;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import com.edu.pcmaster.dto.bottleneck.MlPredictRequest;
import com.edu.pcmaster.dto.bottleneck.MlPredictResponse;
import com.edu.pcmaster.models.BottleneckProfile;
import com.edu.pcmaster.models.BottleneckSide;
import com.edu.pcmaster.models.Product;
import com.edu.pcmaster.repositories.BottleneckProfileRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class BottleneckService {
	private static final Logger log = LoggerFactory.getLogger(BottleneckService.class);

	private final BottleneckProfileRepository bottleneckProfileRepository;
	private final RestClient mlRestClient;
	private final ObjectMapper objectMapper;

	public BottleneckService(
			BottleneckProfileRepository bottleneckProfileRepository,
			@Qualifier("mlRestClient") RestClient mlRestClient,
			ObjectMapper objectMapper) {
		this.bottleneckProfileRepository = bottleneckProfileRepository;
		this.mlRestClient = mlRestClient;
		this.objectMapper = objectMapper;
	}

	/**
	 * Analyze bottleneck using ML service.
	 * Falls back to DB lookup if ML service is unavailable.
	 */
	public MlPredictResponse analyzeWithMl(Product cpu, Product gpu, String resolution) {
		try {
			// Build request with raw specs JSON
			Map<String, Object> cpuSpecs = parseSpecs(cpu);
			Map<String, Object> gpuSpecs = parseSpecs(gpu);

			MlPredictRequest request = new MlPredictRequest(
					new MlPredictRequest.ComponentSpecs(cpu.getName(), cpuSpecs),
					new MlPredictRequest.ComponentSpecs(gpu.getName(), gpuSpecs),
					resolution
			);

			// Call ML service
			MlPredictResponse response = mlRestClient.post()
					.uri("/predict")
					.body(request)
					.retrieve()
					.body(MlPredictResponse.class);

			if (response != null) {
				log.info("ML prediction: CPU={}, GPU={}, res={} → BN={}%, side={}, FPS={}",
						cpu.getName(), gpu.getName(), resolution,
						response.bottleneck_percent(), response.bottleneck_side(), response.fps_estimate());

				// Cache result to DB for future fast lookups
				cacheResult(cpu, gpu, resolution, response);
			}

			return response;

		} catch (Exception e) {
			log.warn("ML service unavailable, falling back to DB lookup: {}", e.getMessage());
			return fallbackToDb(cpu, gpu, resolution);
		}
	}

	/**
	 * Original DB-based lookup (kept as fallback).
	 */
	public BottleneckProfile findProfile(Product cpu, Product gpu, String resolution) {
		return bottleneckProfileRepository.findByCpuProductAndGpuProductAndResolution(cpu, gpu, resolution)
				.orElse(null);
	}

	/**
	 * Fallback: convert DB profile to ML response format.
	 */
	private MlPredictResponse fallbackToDb(Product cpu, Product gpu, String resolution) {
		BottleneckProfile profile = findProfile(cpu, gpu, resolution);
		if (profile != null) {
			return new MlPredictResponse(
					profile.getBottleneckPercent() != null ? profile.getBottleneckPercent().doubleValue() : 0.0,
					profile.getBottleneckSide() != null ? profile.getBottleneckSide().name() : "BALANCED",
					profile.getFpsEstimate() != null ? profile.getFpsEstimate() : 0,
					0.0, 0.0,
					List.of("Kết quả từ dữ liệu có sẵn (ML service không khả dụng)"),
					Map.of("method", "db_fallback")
			);
		}
		return new MlPredictResponse(
				0.0, "BALANCED", 0, 0.0, 0.0,
				List.of("Không tìm thấy dữ liệu bottleneck cho cặp CPU/GPU này"),
				Map.of("method", "not_found")
		);
	}

	/**
	 * Cache ML result into bottleneck_profiles table for faster future lookups.
	 */
	private void cacheResult(Product cpu, Product gpu, String resolution, MlPredictResponse response) {
		try {
			BottleneckProfile profile = bottleneckProfileRepository
					.findByCpuProductAndGpuProductAndResolution(cpu, gpu, resolution)
					.orElse(new BottleneckProfile());

			profile.setCpuProduct(cpu);
			profile.setGpuProduct(gpu);
			profile.setResolution(resolution);
			profile.setBottleneckPercent(BigDecimal.valueOf(response.bottleneck_percent()));
			profile.setBottleneckSide(BottleneckSide.valueOf(response.bottleneck_side()));
			profile.setFpsEstimate(response.fps_estimate());

			bottleneckProfileRepository.save(profile);
		} catch (Exception e) {
			log.warn("Failed to cache bottleneck result: {}", e.getMessage());
		}
	}

	/**
	 * Parse product specs JSON into a Map.
	 * Handles flexible JSON — passes raw key-value pairs to ML service.
	 */
	private Map<String, Object> parseSpecs(Product product) {
		if (product.getSpecsJson() == null || product.getSpecsJson().isEmpty()) {
			return Map.of();
		}
		try {
			return objectMapper.convertValue(product.getSpecsJson(), new TypeReference<>() {});
		} catch (Exception e) {
			log.warn("Failed to parse specs for product {}: {}", product.getId(), e.getMessage());
			return Map.of();
		}
	}
}

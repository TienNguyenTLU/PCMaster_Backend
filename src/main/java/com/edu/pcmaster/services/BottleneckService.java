package com.edu.pcmaster.services;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import com.edu.pcmaster.dto.bottleneck.MlPredictRequest;
import com.edu.pcmaster.dto.bottleneck.MlPredictResponse;
import com.edu.pcmaster.models.Product;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class BottleneckService {
	private static final Logger log = LoggerFactory.getLogger(BottleneckService.class);

	private final RestClient mlRestClient;
	private final ObjectMapper objectMapper;

	public BottleneckService(
			@Qualifier("mlRestClient") RestClient mlRestClient,
			ObjectMapper objectMapper) {
		this.mlRestClient = mlRestClient;
		this.objectMapper = objectMapper;
	}

	public MlPredictResponse analyzeWithMl(Product cpu, Product gpu, String resolution) {
		try {
			Map<String, Object> cpuSpecs = parseSpecs(cpu);
			Map<String, Object> gpuSpecs = parseSpecs(gpu);

			MlPredictRequest request = new MlPredictRequest(
					new MlPredictRequest.ComponentSpecs(cpu.getName(), cpuSpecs),
					new MlPredictRequest.ComponentSpecs(gpu.getName(), gpuSpecs),
					resolution
			);

			MlPredictResponse response = mlRestClient.post()
					.uri("/predict")
					.body(request)
					.retrieve()
					.body(MlPredictResponse.class);

			if (response != null) {
				log.info("ML prediction: CPU={}, GPU={}, res={} → BN={}%, side={}, FPS={}",
						cpu.getName(), gpu.getName(), resolution,
						response.bottleneck_percent(), response.bottleneck_side(), response.fps_estimate());
			}

			return response;

		} catch (Exception e) {
			log.warn("ML service unavailable: {}", e.getMessage());
			return null;
		}
	}

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

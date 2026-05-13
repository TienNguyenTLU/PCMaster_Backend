package com.edu.pcmaster.controllers;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.edu.pcmaster.common.exception.ResourceNotFoundException;
import com.edu.pcmaster.dto.bottleneck.BottleneckResponse;
import com.edu.pcmaster.models.BottleneckProfile;
import com.edu.pcmaster.models.Product;
import com.edu.pcmaster.services.BottleneckService;
import com.edu.pcmaster.services.ProductService;

@RestController
@RequestMapping("/api/bottleneck")
public class BottleneckController {
	private final BottleneckService bottleneckService;
	private final ProductService productService;

	public BottleneckController(BottleneckService bottleneckService, ProductService productService) {
		this.bottleneckService = bottleneckService;
		this.productService = productService;
	}

	@GetMapping
	public BottleneckResponse lookup(@RequestParam Long cpuId,
							 @RequestParam Long gpuId,
							 @RequestParam(defaultValue = "1080p") String res) {
		Product cpu = productService.getById(cpuId);
		Product gpu = productService.getById(gpuId);
		BottleneckProfile profile = bottleneckService.findProfile(cpu, gpu, res);
		if (profile == null) {
			throw new ResourceNotFoundException("Bottleneck profile not found");
		}
		return new BottleneckResponse(
				profile.getId(),
				profile.getCpuProduct() == null ? null : profile.getCpuProduct().getId(),
				profile.getGpuProduct() == null ? null : profile.getGpuProduct().getId(),
				profile.getResolution(),
				profile.getBottleneckPercent(),
				profile.getBottleneckSide(),
				profile.getFpsEstimate()
		);
	}
}


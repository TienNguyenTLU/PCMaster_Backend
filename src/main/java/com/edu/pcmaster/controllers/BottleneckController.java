package com.edu.pcmaster.controllers;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.edu.pcmaster.dto.bottleneck.BottleneckResponse;
import com.edu.pcmaster.dto.bottleneck.MlPredictResponse;
import com.edu.pcmaster.models.BottleneckSide;
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
	public BottleneckResponse analyze(@RequestParam Long cpuId,
									  @RequestParam Long gpuId,
									  @RequestParam(defaultValue = "1080p") String res) {
		Product cpu = productService.getById(cpuId);
		Product gpu = productService.getById(gpuId);

		// Use ML service (with DB fallback)
		MlPredictResponse mlResult = bottleneckService.analyzeWithMl(cpu, gpu, res);

		if (mlResult == null) {
			return new BottleneckResponse(
					null, cpuId, gpuId,
					cpu.getName(), gpu.getName(), res,
					BigDecimal.ZERO, BottleneckSide.BALANCED, 0,
					List.of("Không thể phân tích bottleneck lúc này"),
					Map.of()
			);
		}

		return new BottleneckResponse(
				null,
				cpuId,
				gpuId,
				cpu.getName(),
				gpu.getName(),
				res,
				BigDecimal.valueOf(mlResult.bottleneck_percent()),
				BottleneckSide.valueOf(mlResult.bottleneck_side()),
				mlResult.fps_estimate(),
				mlResult.recommendations(),
				mlResult.details() != null ? mlResult.details() : Map.of()
		);
	}
}

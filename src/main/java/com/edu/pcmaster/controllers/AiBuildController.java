package com.edu.pcmaster.controllers;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.edu.pcmaster.dto.ai.CpuAdviceResponse;
import com.edu.pcmaster.dto.ai.PsuRecommendationResponse;
import com.edu.pcmaster.services.AiBuildService;

@RestController
@RequestMapping("/api/ai")
public class AiBuildController {
	private final AiBuildService aiBuildService;

	public AiBuildController(AiBuildService aiBuildService) {
		this.aiBuildService = aiBuildService;
	}

	@PostMapping("/psu-recommendation")
	public PsuRecommendationResponse getPsuRecommendation(@RequestBody PsuRecommendationRequest request) {
		return aiBuildService.getPsuRecommendation(request.cpu(), request.gpu(), request.ram());
	}

	@GetMapping("/cpu-advice")
	public CpuAdviceResponse getCpuAdvice(@RequestParam String cpuName) {
		return aiBuildService.getCpuAdvice(cpuName);
	}

	public record PsuRecommendationRequest(
			String cpu,
			String gpu,
			String ram
	) {}
}

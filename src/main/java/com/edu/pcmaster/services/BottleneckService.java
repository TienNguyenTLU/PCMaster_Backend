package com.edu.pcmaster.services;

import org.springframework.stereotype.Service;

import com.edu.pcmaster.models.BottleneckProfile;
import com.edu.pcmaster.models.Product;
import com.edu.pcmaster.repositories.BottleneckProfileRepository;

@Service
public class BottleneckService {
	private final BottleneckProfileRepository bottleneckProfileRepository;

	public BottleneckService(BottleneckProfileRepository bottleneckProfileRepository) {
		this.bottleneckProfileRepository = bottleneckProfileRepository;
	}

	public BottleneckProfile findProfile(Product cpu, Product gpu, String resolution) {
		return bottleneckProfileRepository.findByCpuProductAndGpuProductAndResolution(cpu, gpu, resolution)
				.orElse(null);
	}
}


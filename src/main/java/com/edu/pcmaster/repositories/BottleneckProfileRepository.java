package com.edu.pcmaster.repositories;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.edu.pcmaster.models.BottleneckProfile;
import com.edu.pcmaster.models.Product;

public interface BottleneckProfileRepository extends JpaRepository<BottleneckProfile, Long> {
	Optional<BottleneckProfile> findByCpuProductAndGpuProductAndResolution(Product cpu, Product gpu, String resolution);
}


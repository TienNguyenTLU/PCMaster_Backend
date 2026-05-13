package com.edu.pcmaster.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import com.edu.pcmaster.models.PcBuildItem;

public interface PcBuildItemRepository extends JpaRepository<PcBuildItem, Long> {
}


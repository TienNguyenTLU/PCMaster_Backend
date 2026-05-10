package com.edu.pcmaster.repository;

import com.edu.pcmaster.entity.MLModelMetadata;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface MLModelMetadataRepository extends JpaRepository<MLModelMetadata, UUID> {
}

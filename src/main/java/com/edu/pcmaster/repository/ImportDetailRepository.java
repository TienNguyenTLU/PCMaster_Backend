package com.edu.pcmaster.repository;

import com.edu.pcmaster.entity.ImportDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ImportDetailRepository extends JpaRepository<ImportDetail, UUID> {
}

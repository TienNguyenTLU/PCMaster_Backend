package com.edu.pcmaster.repository;

import com.edu.pcmaster.entity.RAGSuggestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface RAGSuggestionRepository extends JpaRepository<RAGSuggestion, UUID> {
}

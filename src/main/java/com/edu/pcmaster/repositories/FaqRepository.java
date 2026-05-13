package com.edu.pcmaster.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.edu.pcmaster.models.Faq;

public interface FaqRepository extends JpaRepository<Faq, Long> {
	List<Faq> findByQuestionContainingIgnoreCase(String keyword);
}


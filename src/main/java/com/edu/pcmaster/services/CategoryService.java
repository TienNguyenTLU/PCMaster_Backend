package com.edu.pcmaster.services;

import java.util.List;

import org.springframework.stereotype.Service;

import com.edu.pcmaster.common.exception.ResourceNotFoundException;
import com.edu.pcmaster.dto.category.CategoryRequest;
import com.edu.pcmaster.models.Category;
import com.edu.pcmaster.repositories.CategoryRepository;

@Service
public class CategoryService {
	private final CategoryRepository categoryRepository;

	public CategoryService(CategoryRepository categoryRepository) {
		this.categoryRepository = categoryRepository;
	}

	public List<Category> findAll() {
		return categoryRepository.findAll();
	}

	public Category getById(Long id) {
		return categoryRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Category not found"));
	}

	public Category create(CategoryRequest request) {
		Category category = new Category();
		apply(category, request);
		return categoryRepository.save(category);
	}

	public Category update(Long id, CategoryRequest request) {
		Category category = getById(id);
		apply(category, request);
		return categoryRepository.save(category);
	}

	public void delete(Long id) {
		Category category = getById(id);
		categoryRepository.delete(category);
	}

	private void apply(Category category, CategoryRequest request) {
		category.setName(request.name());
		category.setSlug(request.slug());
		if (request.parentId() != null) {
			Category parent = getById(request.parentId());
			category.setParent(parent);
		} else {
			category.setParent(null);
		}
	}
}


package com.edu.pcmaster.dto.category;

public record CategoryResponse(
		Long id,
		String name,
		String slug,
		Long parentId
) {
}


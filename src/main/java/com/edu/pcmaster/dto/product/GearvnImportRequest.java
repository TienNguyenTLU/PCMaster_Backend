package com.edu.pcmaster.dto.product;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record GearvnImportRequest(
		@NotBlank(message = "URL không được để trống")
		@Pattern(
				regexp = "^https?://(www\\.)?gearvn\\.com/products/[a-zA-Z0-9\\-_%()+,:]+.*$",
				message = "URL không hợp lệ. Vui lòng nhập link sản phẩm từ gearvn.com"
		)
		String url,

		@NotNull(message = "Category ID không được để trống")
		Long categoryId
) {}

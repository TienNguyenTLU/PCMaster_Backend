package com.edu.pcmaster.dto.chatbot;

import java.math.BigDecimal;

/**
 * DTO sản phẩm rút gọn để hiển thị card đề xuất trong chatbot.
 * Chứa đúng các trường mà Frontend cần để render ProductCard.
 */
public record RecommendedProductDto(
    Long id,
    String name,
    String slug,
    BigDecimal price,
    BigDecimal discountPrice,    // null nếu không có khuyến mãi
    Integer discountPercent,     // null nếu không có khuyến mãi
    String thumbnailUrl,
    Integer stock,
    String categorySlug
) {}

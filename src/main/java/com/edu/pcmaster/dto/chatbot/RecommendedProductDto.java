package com.edu.pcmaster.dto.chatbot;

import java.math.BigDecimal;


public record RecommendedProductDto(
    Long id,
    String name,
    String slug,
    BigDecimal price,
    BigDecimal discountPrice,    
    Integer discountPercent,     
    String thumbnailUrl,
    Integer stock,
    String categorySlug
) {}

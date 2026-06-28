package com.edu.pcmaster.dto.chatbot;

import java.util.List;


public record ChatResponse(
    String message,
    List<RecommendedProductDto> recommendedProducts
) {}

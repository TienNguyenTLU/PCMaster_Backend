package com.edu.pcmaster.dto.chatbot;

import java.util.List;

/**
 * Response trả về cho Frontend sau mỗi lượt chat.
 * message: câu trả lời tư vấn dạng Markdown.
 * recommendedProducts: danh sách sản phẩm đề xuất từ kết quả RAG.
 */
public record ChatResponse(
    String message,
    List<RecommendedProductDto> recommendedProducts
) {}

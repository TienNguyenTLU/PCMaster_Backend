package com.edu.pcmaster.services.chat;

/**
 * Phân loại ý định (intent) của câu hỏi trong RAG Chat.
 */
public enum ChatIntent {

    /** Hỏi thông tin chi tiết về 1 sản phẩm cụ thể (nhắc đúng tên/model) */
    PRODUCT_INFO,

    /** Gợi ý sản phẩm theo giá, thông số, mục đích sử dụng */
    PRODUCT_SUGGEST,

    /** Xây dựng cấu hình PC hoàn chỉnh */
    BUILD_CONFIG,

    /** Câu hỏi chung: chào hỏi, hỏi về cửa hàng, FAQ */
    GENERAL
}

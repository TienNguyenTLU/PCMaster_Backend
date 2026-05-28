package com.edu.pcmaster.dto.chatbot;

/**
 * DTO đại diện cho một lượt tin nhắn trong lịch sử hội thoại.
 * role: "user" (khách hàng) hoặc "assistant" (trợ lý AI).
 */
public record ChatMessageDto(
    String role,
    String content
) {}

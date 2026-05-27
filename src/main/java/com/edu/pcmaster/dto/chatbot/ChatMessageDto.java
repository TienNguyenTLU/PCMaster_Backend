package com.edu.pcmaster.dto.chatbot;

/**
 * DTO đại diện cho một tin nhắn trong lịch sử trò chuyện gửi từ Frontend.
 * Sử dụng một record cụ thể thay cho JsonNode trừu tượng để Jackson có thể deserialize an toàn tuyệt đối, tránh lỗi 400.
 */
public record ChatMessageDto(
    String role,
    String content
) {}

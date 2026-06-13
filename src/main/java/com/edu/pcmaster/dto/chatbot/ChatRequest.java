package com.edu.pcmaster.dto.chatbot;

import java.util.List;

/**
 * Request body gửi từ Frontend khi người dùng chat.
 * message: câu hỏi hiện tại.
 * history: lịch sử hội thoại trước đó (có thể null hoặc rỗng).
 */
public record ChatRequest(
    String message,
    List<ChatMessageDto> history,
    String mode
) {}

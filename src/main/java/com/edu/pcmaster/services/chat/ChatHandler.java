package com.edu.pcmaster.services.chat;

import com.edu.pcmaster.dto.chatbot.ChatMessageDto;
import com.edu.pcmaster.dto.chatbot.ChatResponse;

import java.util.List;

/**
 * Interface chung cho tất cả chat handler.
 * Mỗi handler xử lý một loại intent khác nhau.
 */
public interface ChatHandler {

    /**
     * Xử lý câu hỏi và trả về response.
     *
     * @param message tin nhắn từ người dùng
     * @param history lịch sử hội thoại
     * @return ChatResponse chứa message AI và danh sách sản phẩm gợi ý
     */
    ChatResponse handle(String message, List<ChatMessageDto> history);
}

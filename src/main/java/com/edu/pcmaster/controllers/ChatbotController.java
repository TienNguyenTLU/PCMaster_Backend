package com.edu.pcmaster.controllers;

import java.util.List;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.edu.pcmaster.dto.chatbot.ChatMessageDto;
import com.edu.pcmaster.services.ChatbotService;

@RestController
@RequestMapping("/api/chat")
public class ChatbotController {

    private final ChatbotService chatbotService;

    public ChatbotController(ChatbotService chatbotService) {
        this.chatbotService = chatbotService;
    }

    /**
     * DTO đại diện cho yêu cầu trò chuyện gửi từ Frontend.
     */
    public record ChatRequest(
        String message,
        List<ChatMessageDto> history
    ) {}

    /**
     * Điểm tiếp nhận tin nhắn chat tư vấn (cho phép truy cập công khai).
     */
    @PostMapping
    public ChatbotService.ChatbotResponse chat(@RequestBody ChatRequest request) {
        // Nếu danh sách history bị null, khởi tạo danh sách rỗng để tránh NullPointerException
        List<ChatMessageDto> chatHistory = request.history() != null ? request.history() : List.of();
        return chatbotService.processChatMessage(request.message(), chatHistory);
    }
}

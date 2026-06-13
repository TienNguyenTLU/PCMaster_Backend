package com.edu.pcmaster.controllers;

import com.edu.pcmaster.dto.chatbot.ChatMessageDto;
import com.edu.pcmaster.dto.chatbot.ChatRequest;
import com.edu.pcmaster.dto.chatbot.ChatResponse;
import com.edu.pcmaster.services.RagChatService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Controller tiếp nhận tin nhắn chat từ Frontend.
 * Endpoint công khai, không yêu cầu xác thực JWT.
 * POST /api/chat → RAG pipeline → ChatResponse
 */
@RestController
@RequestMapping("/api/chat")
public class ChatbotController {

    private final RagChatService ragChatService;

    public ChatbotController(RagChatService ragChatService) {
        this.ragChatService = ragChatService;
    }

    /**
     * Nhận tin nhắn từ người dùng và trả về câu trả lời AI kèm sản phẩm đề xuất.
     */
    @PostMapping
    public ResponseEntity<ChatResponse> chat(@RequestBody ChatRequest request) {
        if (request.message() == null || request.message().isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        List<ChatMessageDto> history = request.history() != null ? request.history() : List.of();
        String mode = request.mode() != null ? request.mode() : "consult";
        ChatResponse response = ragChatService.chat(request.message().trim(), history, mode);
        return ResponseEntity.ok(response);
    }
}

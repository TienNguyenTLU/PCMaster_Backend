package com.edu.pcmaster.services;

import com.edu.pcmaster.dto.chatbot.ChatMessageDto;
import com.edu.pcmaster.dto.chatbot.ChatResponse;
import com.edu.pcmaster.services.chat.BuildConfigHandler;
import com.edu.pcmaster.services.chat.ChatIntent;
import com.edu.pcmaster.services.chat.GeneralChatHandler;
import com.edu.pcmaster.services.chat.IntentClassifier;
import com.edu.pcmaster.services.chat.ProductInfoHandler;
import com.edu.pcmaster.services.chat.ProductSuggestHandler;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * RAG Chat Service — Orchestrator.
 *
 * Phân loại ý định (intent) của câu hỏi rồi delegate sang handler chuyên biệt:
 * - PRODUCT_INFO    → ProductInfoHandler     (thông tin chi tiết 1 sản phẩm)
 * - PRODUCT_SUGGEST → ProductSuggestHandler   (gợi ý sản phẩm theo yêu cầu)
 * - BUILD_CONFIG    → BuildConfigHandler      (xây dựng cấu hình PC)
 * - GENERAL         → GeneralChatHandler      (chào hỏi, FAQ)
 */
@Service
public class RagChatService {

    private final IntentClassifier intentClassifier;
    private final ProductInfoHandler productInfoHandler;
    private final ProductSuggestHandler productSuggestHandler;
    private final BuildConfigHandler buildConfigHandler;
    private final GeneralChatHandler generalChatHandler;

    public RagChatService(IntentClassifier intentClassifier,
                          ProductInfoHandler productInfoHandler,
                          ProductSuggestHandler productSuggestHandler,
                          BuildConfigHandler buildConfigHandler,
                          GeneralChatHandler generalChatHandler) {
        this.intentClassifier = intentClassifier;
        this.productInfoHandler = productInfoHandler;
        this.productSuggestHandler = productSuggestHandler;
        this.buildConfigHandler = buildConfigHandler;
        this.generalChatHandler = generalChatHandler;
    }

    /**
     * Entry point cho RAG Chat.
     * Phân loại intent → delegate sang handler tương ứng.
     *
     * @param message tin nhắn từ người dùng
     * @param history lịch sử hội thoại
     * @param mode    mode từ frontend ("build" hoặc "consult")
     * @return ChatResponse chứa AI response + sản phẩm gợi ý
     */
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public ChatResponse chat(String message, List<ChatMessageDto> history, String mode) {
        // 1. Phân loại intent
        ChatIntent intent = intentClassifier.classify(message, mode);
        System.out.printf("[RAG Chat] Message='%s' | Mode='%s' | Intent=%s%n", message, mode, intent);

        // 2. Delegate sang handler tương ứng
        return switch (intent) {
            case PRODUCT_INFO    -> productInfoHandler.handle(message, history);
            case PRODUCT_SUGGEST -> productSuggestHandler.handle(message, history);
            case BUILD_CONFIG    -> buildConfigHandler.handle(message, history);
            case GENERAL         -> generalChatHandler.handle(message, history);
        };
    }
}

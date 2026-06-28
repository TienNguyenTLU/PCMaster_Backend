package com.edu.pcmaster.controllers;

import com.edu.pcmaster.services.EmbeddingIngestionService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;


@RestController
@RequestMapping("/api/admin/chatbot")
@PreAuthorize("hasRole('ADMIN')")
public class AdminChatbotController {

    private final EmbeddingIngestionService ingestionService;

    public AdminChatbotController(EmbeddingIngestionService ingestionService) {
        this.ingestionService = ingestionService;
    }

    
    @PostMapping("/reindex")
    public ResponseEntity<Map<String, Object>> reindex() {
        long startTime = System.currentTimeMillis();
        int count = ingestionService.reindexAll();
        long durationMs = System.currentTimeMillis() - startTime;

        return ResponseEntity.ok(Map.of(
                "success", true,
                "indexedProducts", count,
                "durationMs", durationMs,
                "timestamp", Instant.now().toString(),
                "message", String.format("Đã reindex thành công %d sản phẩm vào PGVector (%.1f giây)",
                        count, durationMs / 1000.0)
        ));
    }

    
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> status() {
        long indexableCount = ingestionService.getIndexableProductCount();

        return ResponseEntity.ok(Map.of(
                "indexableProducts", indexableCount,
                "ollamaBaseUrl", "http://localhost:11434",
                "embeddingModel", "nomic-embed-text",
                "chatModel", "qwen2.5:7b",
                "vectorDimensions", 768,
                "message", String.format("Có %d sản phẩm còn hàng có thể index. " +
                        "Gọi POST /api/admin/chatbot/reindex để cập nhật.", indexableCount)
        ));
    }
}

package com.edu.pcmaster.services;

import com.edu.pcmaster.dto.chatbot.ChatMessageDto;
import com.edu.pcmaster.dto.chatbot.ChatResponse;
import com.edu.pcmaster.dto.chatbot.RecommendedProductDto;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service điều phối toàn bộ RAG (Retrieval-Augmented Generation) pipeline.
 *
 * Luồng xử lý mỗi lượt chat:
 * 1. RETRIEVAL: Tìm kiếm ngữ nghĩa top-K sản phẩm liên quan từ PGVector
 * 2. AUGMENTATION: Ghép context sản phẩm vào system prompt
 * 3. GENERATION: Gửi toàn bộ prompt + lịch sử hội thoại sang Ollama để sinh câu trả lời
 * 4. EXTRACTION: Trích xuất danh sách sản phẩm từ kết quả tìm kiếm để render card UI
 */
@Service
public class RagChatService {

    private final ChatModel chatModel;
    private final VectorStore vectorStore;

    // Số kết quả tìm kiếm ngữ nghĩa tối đa mỗi lượt (top-K)
    private static final int TOP_K = 6;

    // Ngưỡng similarity tối thiểu (0.0–1.0) – lọc sản phẩm kém liên quan
    // COSINE_DISTANCE: score càng cao = càng gần → dùng threshold ~0.45
    private static final double SIMILARITY_THRESHOLD = 0.45;

    // Số lượt hội thoại lịch sử tối đa được gửi kèm (giới hạn context window)
    private static final int MAX_HISTORY_TURNS = 10;

    /**
     * System Prompt định hình vai trò và quy tắc của trợ lý AI PCMaster.
     * {context} sẽ được thay thế bằng danh sách sản phẩm thực tế từ RAG.
     */
    private static final String SYSTEM_PROMPT_TEMPLATE = """
            Bạn là chuyên gia tư vấn phần cứng và lắp ráp PC chuyên nghiệp, thân thiện tại cửa hàng PCMaster (Hệ thống PC & Linh kiện chính hãng hàng đầu Việt Nam).
            
            NHIỆM VỤ: Trò chuyện thân thiện, tư vấn nhiệt tình và đề xuất sản phẩm phù hợp với nhu cầu và ngân sách của khách hàng.
            
            QUY TẮC BẮT BUỘC:
            1. CHỈ được tư vấn các sản phẩm có trong danh sách thực tế bên dưới (dữ liệu kho hàng hiện tại).
            2. TUYỆT ĐỐI KHÔNG bịa đặt tên sản phẩm, mã linh kiện, hoặc giá tiền không có trong danh sách.
            3. Sử dụng tiếng Việt tự nhiên, chuyên nghiệp. Định dạng câu trả lời bằng Markdown (in đậm, gạch đầu dòng, bảng so sánh ngắn gọn).
            4. Nếu không có sản phẩm phù hợp yêu cầu, lịch sự giải thích và đề xuất khách điều chỉnh ngân sách hoặc yêu cầu cụ thể hơn.
            5. Khi đề xuất sản phẩm, hãy nêu rõ tên, giá, và lý do phù hợp với nhu cầu khách hàng.
            
            === DANH SÁCH SẢN PHẨM THỰC TẾ TRONG KHO (CÒN HÀNG) ===
            {context}
            === HẾT DANH SÁCH ===
            """;

    public RagChatService(ChatModel chatModel, VectorStore vectorStore) {
        this.chatModel = chatModel;
        this.vectorStore = vectorStore;
    }

    /**
     * Xử lý một lượt chat theo pipeline RAG hoàn chỉnh.
     *
     * @param message Câu hỏi/yêu cầu hiện tại của người dùng
     * @param history Lịch sử hội thoại trước đó (có thể null)
     * @return ChatResponse gồm câu trả lời AI và danh sách sản phẩm đề xuất
     */
    public ChatResponse chat(String message, List<ChatMessageDto> history) {

        // ── BƯỚC 1: RETRIEVAL ─────────────────────────────────────────────────
        // Tìm kiếm ngữ nghĩa các sản phẩm phù hợp nhất với câu hỏi
        List<Document> relevantDocs;
        try {
            relevantDocs = vectorStore.similaritySearch(
                    SearchRequest.builder()
                            .query(message)
                            .topK(TOP_K)
                            .similarityThreshold(SIMILARITY_THRESHOLD)
                            .build()
            );
        } catch (Exception e) {
            System.err.println("[RAG] Vector search failed: " + e.getMessage());
            relevantDocs = List.of();
        }

        // ── BƯỚC 2: AUGMENTATION ──────────────────────────────────────────────
        // Ghép context sản phẩm tìm được vào system prompt
        String context = buildContext(relevantDocs);
        String systemContent = SYSTEM_PROMPT_TEMPLATE.replace("{context}", context);

        // ── BƯỚC 3: BUILD PROMPT ──────────────────────────────────────────────
        List<Message> messages = new ArrayList<>();
        messages.add(new SystemMessage(systemContent));

        // Thêm lịch sử hội thoại (bỏ qua các tin nhắn 'assistant' đứng đầu)
        if (history != null && !history.isEmpty()) {
            int startIdx = Math.max(0, history.size() - MAX_HISTORY_TURNS);
            boolean firstUserFound = false;
            for (int i = startIdx; i < history.size(); i++) {
                ChatMessageDto msg = history.get(i);
                if ("assistant".equalsIgnoreCase(msg.role()) && !firstUserFound) {
                    continue; // Bỏ qua các tin nhắn assistant đứng trước tin user đầu tiên
                }
                if ("user".equalsIgnoreCase(msg.role())) {
                    firstUserFound = true;
                    messages.add(new UserMessage(msg.content()));
                } else if ("assistant".equalsIgnoreCase(msg.role())) {
                    messages.add(new AssistantMessage(msg.content()));
                }
            }
        }

        // Thêm câu hỏi hiện tại của người dùng
        messages.add(new UserMessage(message));

        // ── BƯỚC 4: GENERATION ────────────────────────────────────────────────
        // Gọi Ollama để sinh câu trả lời
        String aiResponse;
        try {
            Prompt prompt = new Prompt(messages);
            aiResponse = chatModel.call(prompt)
                    .getResult()
                    .getOutput()
                    .getText();
        } catch (Exception e) {
            System.err.println("[RAG] Ollama call failed: " + e.getMessage());
            aiResponse = "Xin lỗi bạn, trợ lý AI đang gặp sự cố kết nối. " +
                        "Vui lòng đảm bảo Ollama đang chạy và thử lại sau! 🛠️";
        }

        // ── BƯỚC 5: EXTRACT PRODUCTS ──────────────────────────────────────────
        // Chỉ trả về product cards cho sản phẩm AI THỰC SỰ đề cập trong câu trả lời.
        // Tránh hiển thị main/case khi user chỉ hỏi về VGA.
        boolean aiIndicatesNoProducts = aiResponse.contains("không tìm thấy")
                || aiResponse.contains("không có sản phẩm")
                || aiResponse.contains("ngoài phạm vi")
                || aiResponse.contains("Xin lỗi bạn, trợ lý AI đang gặp sự cố");

        List<RecommendedProductDto> recommended;
        if (aiIndicatesNoProducts || relevantDocs.isEmpty()) {
            recommended = List.of();
        } else {
            // Normalize AI response một lần để so sánh
            String aiResponseLower = aiResponse.toLowerCase();

            recommended = relevantDocs.stream()
                    .map(doc -> buildProductDto(doc.getMetadata()))
                    .filter(p -> p != null)
                    .filter(p -> isProductMentionedInResponse(p.name(), aiResponseLower))
                    .collect(Collectors.toList());
        }

        return new ChatResponse(aiResponse, recommended);
    }

    /**
     * Ghép nội dung các Document tìm được thành một chuỗi context cho LLM.
     */
    private String buildContext(List<Document> docs) {
        if (docs == null || docs.isEmpty()) {
            return "Hiện không tìm thấy sản phẩm phù hợp trong kho. " +
                   "Lưu ý: Hệ thống cần được reindex trước khi sử dụng.";
        }
        return docs.stream()
                .map(Document::getText)
                .collect(Collectors.joining("\n\n---\n\n"));
    }

    /**
     * Tái tạo RecommendedProductDto từ metadata lưu trong vector store.
     */
    private RecommendedProductDto buildProductDto(Map<String, Object> metadata) {
        if (metadata == null) return null;
        try {
            Long id = ((Number) metadata.get("productId")).longValue();
            String name = (String) metadata.getOrDefault("name", "");
            String slug = (String) metadata.getOrDefault("slug", "");
            BigDecimal price = new BigDecimal(metadata.getOrDefault("price", "0").toString());

            int discountPct = ((Number) metadata.getOrDefault("discountPercent", 0)).intValue();
            Integer discountPercent = discountPct > 0 ? discountPct : null;

            BigDecimal discountPrice = null;
            if (discountPct > 0 && metadata.containsKey("discountPrice")) {
                discountPrice = new BigDecimal(metadata.get("discountPrice").toString());
            }

            String thumbnailUrl = (String) metadata.get("thumbnailUrl");
            if (thumbnailUrl != null && thumbnailUrl.isBlank()) thumbnailUrl = null;

            Integer stock = ((Number) metadata.getOrDefault("stock", 0)).intValue();

            return new RecommendedProductDto(id, name, slug, price,
                    discountPrice, discountPercent, thumbnailUrl, stock);
        } catch (Exception e) {
            System.err.println("[RAG] Failed to parse product metadata: " + e.getMessage());
            return null;
        }
    }

    /**
     * Kiểm tra xem sản phẩm có được AI đề cập trong câu trả lời không.
     *
     * Logic: Tách tên sản phẩm thành các token đặc trưng (≥3 ký tự, bỏ noise words).
     * Sản phẩm được coi là "được đề cập" nếu AI response chứa ≥50% số token đặc trưng
     * HOẶC chứa bất kỳ token nào là mã model (chứa số, ví dụ: "4060", "RTX", "B760M").
     *
     * Ví dụ:
     *   Tên: "GIGABYTE GeForce RTX 4060 EAGLE OC 8G"
     *   Tokens đặc trưng: [gigabyte, geforce, rtx, 4060, eagle]
     *   AI response chứa "RTX 4060" → match token "rtx" + "4060" → ĐỀ XUẤT ✓
     *   AI response chỉ nói "card đồ họa" → không match token nào → BỎ QUA ✗
     */
    private boolean isProductMentionedInResponse(String productName, String aiResponseLower) {
        if (productName == null || productName.isBlank()) return false;

        // Các từ phổ biến không mang tính nhận dạng sản phẩm
        var noiseWords = java.util.Set.of(
                "the", "for", "and", "with", "pro", "max", "plus", "super",
                "edition", "series", "gaming", "desktop", "laptop"
        );

        // Tách tên thành token, lọc noise và token quá ngắn
        String[] tokens = productName.toLowerCase().split("[\\s\\-/()\\[\\],]+");
        List<String> significantTokens = new ArrayList<>();
        for (String token : tokens) {
            if (token.length() >= 3 && !noiseWords.contains(token)) {
                significantTokens.add(token);
            }
        }

        if (significantTokens.isEmpty()) return false;

        // Đếm bao nhiêu token xuất hiện trong AI response
        int matchCount = 0;
        boolean hasModelNumberMatch = false;
        for (String token : significantTokens) {
            if (aiResponseLower.contains(token)) {
                matchCount++;
                // Token chứa số thường là mã model (4060, 7800xt, b760m) → rất đặc trưng
                if (token.matches(".*\\d+.*")) {
                    hasModelNumberMatch = true;
                }
            }
        }

        // Match nếu:
        // 1. Có match mã model (rất chính xác, ví dụ: "4060" chỉ khớp đúng VGA RTX 4060)
        // 2. HOẶC ≥50% token đặc trưng được đề cập (phòng trường hợp tên không có số)
        double matchRatio = (double) matchCount / significantTokens.size();
        return hasModelNumberMatch || matchRatio >= 0.5;
    }
}

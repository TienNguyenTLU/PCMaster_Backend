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
 * Service điều phối RAG (Retrieval-Augmented Generation) pipeline.
 *
 * Pipeline:
 * 1. RETRIEVAL  → Tìm sản phẩm liên quan từ PGVector
 * 2. AUGMENT    → Ghép catalog sản phẩm vào system prompt
 * 3. GENERATE   → Gọi Ollama (qwen2.5:7b) sinh câu trả lời
 * 4. EXTRACT    → Trích xuất sản phẩm được đề cập để render card UI
 */
@Service
public class RagChatService {

    private final ChatModel chatModel;
    private final VectorStore vectorStore;

    private static final int TOP_K = 6;
    private static final double SIMILARITY_THRESHOLD = 0.45;
    private static final int MAX_HISTORY_TURNS = 6;
    private static final int BUILD_PER_CATEGORY_LIMIT = 3;

    // ─── SYSTEM PROMPTS (ngắn gọn, directive) ────────────────────────────────

    /**
     * Prompt chế độ TƯ VẤN SẢN PHẨM.
     * Ngắn gọn, buộc AI chỉ dùng sản phẩm trong catalog.
     */
    private static final String SYSTEM_PROMPT_TEMPLATE = """
            Bạn là trợ lý bán hàng tại cửa hàng linh kiện PCMaster.
            
            QUY TẮC TUYỆT ĐỐI:
            - CHỈ đề xuất sản phẩm có trong CATALOG bên dưới. CẤM bịa tên hoặc giá sản phẩm.
            - Sao chép CHÍNH XÁC tên và giá từ catalog. Không viết tắt, không đổi tên.
            - Nếu không có sản phẩm phù hợp, nói thẳng "Cửa hàng chưa có sản phẩm phù hợp".
            - Trả lời NGẮN GỌN bằng tiếng Việt: liệt kê sản phẩm phù hợp kèm giá, 1 dòng lý do.
            - KHÔNG viết bài đánh giá dài, KHÔNG so sánh chi tiết, KHÔNG giải thích thông số.
            
            CATALOG SẢN PHẨM:
            {catalog}
            """;

    /**
     * Prompt chế độ XÂY DỰNG CẤU HÌNH.
     * Buộc AI chỉ chọn linh kiện từ catalog, output dạng danh sách.
     */
    private static final String SYSTEM_PROMPT_BUILD_TEMPLATE = """
            Bạn là chuyên gia lắp ráp PC tại cửa hàng PCMaster. Nhiệm vụ: chọn linh kiện xây dựng cấu hình PC.
            
            QUY TẮC TUYỆT ĐỐI:
            - CHỈ chọn linh kiện có trong CATALOG bên dưới. CẤM bịa tên hoặc giá.
            - Sao chép CHÍNH XÁC tên và giá từ catalog.
            - Nếu thiếu loại linh kiện nào, nói thẳng "Cửa hàng chưa có [loại] phù hợp".
            - Đảm bảo tương thích: socket CPU khớp mainboard, loại RAM đúng (DDR4/DDR5), nguồn đủ công suất.
            
            FORMAT TRẢ LỜI (BẮT BUỘC):
            Liệt kê theo format sau, KHÔNG thêm đánh giá hay giải thích dài:
            - **CPU**: [tên] — [giá]
            - **Mainboard**: [tên] — [giá]
            - **RAM**: [tên] — [giá]
            - **VGA**: [tên] — [giá]
            - **SSD**: [tên] — [giá]
            - **PSU**: [tên] — [giá]
            - **Case**: [tên] — [giá]
            - **Tản nhiệt**: [tên] — [giá]
            **Tổng cộng: [tổng giá]**
            
            Nếu có lưu ý tương thích quan trọng, viết 1 dòng ngắn ở cuối.
            Nhắc người dùng nhấn nút "+" để thêm linh kiện vào cấu hình.
            
            CATALOG LINH KIỆN:
            {catalog}
            """;

    public RagChatService(ChatModel chatModel, VectorStore vectorStore) {
        this.chatModel = chatModel;
        this.vectorStore = vectorStore;
    }

    /**
     * Xử lý một lượt chat theo pipeline RAG.
     */
    public ChatResponse chat(String message, List<ChatMessageDto> history, String mode) {

        // ── BƯỚC 1: RETRIEVAL ─────────────────────────────────────────────────
        List<Document> relevantDocs;
        try {
            int topKLimit = "build".equalsIgnoreCase(mode) ? 60 : TOP_K;
            List<Document> rawDocs = vectorStore.similaritySearch(
                    SearchRequest.builder()
                            .query(message)
                            .topK(topKLimit)
                            .similarityThreshold(SIMILARITY_THRESHOLD)
                            .build()
            );

            if ("build".equalsIgnoreCase(mode)) {
                // Group by category, lấy top-N mỗi loại linh kiện
                java.util.Map<String, List<Document>> grouped = new java.util.HashMap<>();
                for (Document doc : rawDocs) {
                    String catSlug = (String) doc.getMetadata().get("categorySlug");
                    if (catSlug == null) continue;
                    String cleanCat = catSlug.toLowerCase();
                    if (cleanCat.contains("laptop") || cleanCat.contains("pc-system") || cleanCat.contains("pc-gear")) {
                        continue;
                    }
                    grouped.computeIfAbsent(cleanCat, k -> new ArrayList<>()).add(doc);
                }

                relevantDocs = new ArrayList<>();
                for (var entry : grouped.entrySet()) {
                    List<Document> catDocs = entry.getValue();
                    relevantDocs.addAll(catDocs.subList(0, Math.min(BUILD_PER_CATEGORY_LIMIT, catDocs.size())));
                }
            } else {
                relevantDocs = rawDocs;
            }
        } catch (Exception e) {
            System.err.println("[RAG] Vector search failed: " + e.getMessage());
            relevantDocs = List.of();
        }

        // ── BƯỚC 2: AUGMENTATION ──────────────────────────────────────────────
        // Ghép catalog sản phẩm duy nhất vào prompt (không gửi trùng lặp)
        String catalog = buildCatalog(relevantDocs);

        String systemContent = "build".equalsIgnoreCase(mode)
                ? SYSTEM_PROMPT_BUILD_TEMPLATE.replace("{catalog}", catalog)
                : SYSTEM_PROMPT_TEMPLATE.replace("{catalog}", catalog);

        // ── BƯỚC 3: BUILD PROMPT ──────────────────────────────────────────────
        List<Message> messages = new ArrayList<>();
        messages.add(new SystemMessage(systemContent));

        // Thêm lịch sử hội thoại (giới hạn để giữ context nhỏ)
        if (history != null && !history.isEmpty()) {
            int startIdx = Math.max(0, history.size() - MAX_HISTORY_TURNS);
            boolean firstUserFound = false;
            for (int i = startIdx; i < history.size(); i++) {
                ChatMessageDto msg = history.get(i);
                if ("assistant".equalsIgnoreCase(msg.role()) && !firstUserFound) continue;
                if ("user".equalsIgnoreCase(msg.role())) {
                    firstUserFound = true;
                    messages.add(new UserMessage(msg.content()));
                } else if ("assistant".equalsIgnoreCase(msg.role())) {
                    messages.add(new AssistantMessage(msg.content()));
                }
            }
        }

        messages.add(new UserMessage(message));

        // ── BƯỚC 4: GENERATION ────────────────────────────────────────────────
        String aiResponse;
        try {
            Prompt prompt = new Prompt(messages);
            aiResponse = chatModel.call(prompt)
                    .getResult()
                    .getOutput()
                    .getText();
        } catch (Exception e) {
            System.err.println("[RAG] Ollama call failed: " + e.getMessage());
            aiResponse = "Xin lỗi, trợ lý AI đang gặp sự cố kết nối. Vui lòng thử lại sau! 🛠️";
        }

        // ── BƯỚC 5: EXTRACT PRODUCTS ──────────────────────────────────────────
        boolean aiIndicatesNoProducts = aiResponse.contains("không tìm thấy")
                || aiResponse.contains("không có sản phẩm")
                || aiResponse.contains("chưa có sản phẩm")
                || aiResponse.contains("chưa có linh kiện")
                || aiResponse.contains("sự cố kết nối");

        List<RecommendedProductDto> recommended;
        if (aiIndicatesNoProducts || relevantDocs.isEmpty()) {
            recommended = List.of();
        } else {
            String aiLower = aiResponse.toLowerCase();
            recommended = relevantDocs.stream()
                    .map(doc -> buildProductDto(doc.getMetadata()))
                    .filter(p -> p != null)
                    .filter(p -> isProductMentionedInResponse(p.name(), aiLower))
                    .collect(Collectors.toList());
        }

        return new ChatResponse(aiResponse, recommended);
    }

    // ─── CATALOG BUILDER ──────────────────────────────────────────────────────

    /**
     * Xây dựng catalog sản phẩm duy nhất để inject vào prompt.
     * Format gọn: tên, giá, thông số chính — đủ để AI tư vấn chính xác.
     * KHÔNG gửi context trùng lặp (trước đây gửi cả {context} và {product_names}).
     */
    private String buildCatalog(List<Document> docs) {
        if (docs == null || docs.isEmpty()) {
            return "(Không có sản phẩm khả dụng. Cần reindex trước khi sử dụng.)";
        }
        StringBuilder sb = new StringBuilder();
        int idx = 1;
        for (Document doc : docs) {
            Map<String, Object> meta = doc.getMetadata();
            String name = (String) meta.getOrDefault("name", "");
            String price = meta.getOrDefault("price", "0").toString();
            String categorySlug = (String) meta.getOrDefault("categorySlug", "");
            String brandName = (String) meta.getOrDefault("brandName", "");
            int stock = ((Number) meta.getOrDefault("stock", 0)).intValue();
            int discountPct = ((Number) meta.getOrDefault("discountPercent", 0)).intValue();
            String specsText = (String) meta.getOrDefault("specsText", "");

            sb.append(idx++).append(") ").append(name);
            sb.append(" [").append(categorySlug).append("]");
            if (!brandName.isEmpty()) sb.append(" (").append(brandName).append(")");
            sb.append("\n");

            // Giá
            sb.append("   Giá: ").append(fmtPrice(price));
            if (discountPct > 0 && meta.containsKey("discountPrice")) {
                sb.append(" → ").append(fmtPrice(meta.get("discountPrice").toString()));
                sb.append(" (-").append(discountPct).append("%)");
            }
            sb.append(" | Kho: ").append(stock).append("\n");

            // Thông số (gọn)
            if (!specsText.isEmpty()) {
                // Lấy tối đa 5 dòng specs quan trọng nhất
                String[] specLines = specsText.split("\n");
                int lineCount = 0;
                for (String line : specLines) {
                    if (!line.isBlank() && lineCount < 5) {
                        sb.append("   ").append(line.trim()).append("\n");
                        lineCount++;
                    }
                }
            }
            sb.append("\n");
        }
        return sb.toString().trim();
    }

    private String fmtPrice(String priceStr) {
        try {
            return String.format("%,.0f₫", new BigDecimal(priceStr));
        } catch (Exception e) {
            return priceStr;
        }
    }

    // ─── PRODUCT DTO BUILDER ──────────────────────────────────────────────────

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
            String categorySlug = (String) metadata.getOrDefault("categorySlug", "");

            return new RecommendedProductDto(id, name, slug, price,
                    discountPrice, discountPercent, thumbnailUrl, stock, categorySlug);
        } catch (Exception e) {
            System.err.println("[RAG] Failed to parse product metadata: " + e.getMessage());
            return null;
        }
    }

    // ─── PRODUCT MENTION DETECTOR ─────────────────────────────────────────────

    /**
     * Kiểm tra sản phẩm có được AI đề cập trong response không.
     * Dùng token matching: model number (chứa số) hoặc ≥50% token đặc trưng.
     */
    private boolean isProductMentionedInResponse(String productName, String aiResponseLower) {
        if (productName == null || productName.isBlank()) return false;

        var noiseWords = java.util.Set.of(
                "the", "for", "and", "with", "pro", "max", "plus", "super",
                "edition", "series", "gaming", "desktop", "laptop"
        );

        String[] tokens = productName.toLowerCase().split("[\\s\\-/()\\[\\],]+");
        List<String> sigTokens = new ArrayList<>();
        for (String t : tokens) {
            if (t.length() >= 3 && !noiseWords.contains(t)) sigTokens.add(t);
        }
        if (sigTokens.isEmpty()) return false;

        int matchCount = 0;
        boolean hasModelMatch = false;
        for (String t : sigTokens) {
            if (aiResponseLower.contains(t)) {
                matchCount++;
                if (t.matches(".*\\d+.*")) hasModelMatch = true;
            }
        }

        return hasModelMatch || (double) matchCount / sigTokens.size() >= 0.5;
    }
}

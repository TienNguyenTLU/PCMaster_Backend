package com.edu.pcmaster.services.chat;

import com.edu.pcmaster.dto.chatbot.ChatMessageDto;
import com.edu.pcmaster.dto.chatbot.ChatResponse;
import com.edu.pcmaster.dto.chatbot.RecommendedProductDto;
import com.edu.pcmaster.repositories.ProductRepository;
import com.edu.pcmaster.services.ProductDetailsFunction;
import com.edu.pcmaster.services.ProductService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.document.Document;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Handler cho câu hỏi xây dựng cấu hình PC hoàn chỉnh.
 * Ví dụ: "Build PC gaming 25 triệu", "Ráp máy đồ họa 30tr"
 */
@Component
public class BuildConfigHandler implements ChatHandler {

    private final ChatModel chatModel;
    private final VectorStore vectorStore;
    private final ProductRepository productRepository;
    private final ProductService productService;
    private final ObjectMapper objectMapper;
    private final CatalogBuilder catalogBuilder;

    private static final int TOP_K = 150;
    private static final double SIMILARITY_THRESHOLD = 0.20;
    private static final int PER_CATEGORY_LIMIT = 3;
    private static final int MAX_HISTORY_TURNS = 6;

    /** Categories cần loại bỏ khỏi kết quả build (laptop, PC nguyên bộ, gear) */
    private static final List<String> EXCLUDED_CATEGORIES = List.of(
            "laptop", "pc-system", "pc-gear", "monitor", "man-hinh",
            "ban-phim", "keyboard", "chuot", "mouse", "tai-nghe", "headset",
            "loa", "speaker"
    );

    private static final String SYSTEM_PROMPT = """
            Bạn là chuyên gia lắp ráp PC tại cửa hàng PCMaster. Nhiệm vụ: chọn linh kiện xây dựng cấu hình PC tối ưu.

            PHONG CÁCH GIAO TIẾP:
            - Xưng "mình", gọi khách là "bạn"
            - Giải thích lý do chọn từng linh kiện một cách dễ hiểu
            - Dùng emoji (🖥️💪🔧⚡🎮) tạo sự thân thiện
            - Nhấn mạnh tính tương thích và cân bằng hiệu năng

            QUY TẮC BẮT BUỘC:
            1. CHỈ chọn linh kiện có trong CATALOG. CẤM bịa tên sản phẩm.
            2. BẮT BUỘC gọi `getProductDetails` cho TỪNG linh kiện để lấy giá, tồn kho thực tế.
            3. Nếu stock = 0 → chọn linh kiện thay thế cùng loại.
            4. Đảm bảo TƯƠNG THÍCH:
               - Socket CPU khớp với Mainboard
               - Loại RAM đúng (DDR4/DDR5) với CPU & Mainboard
               - PSU đủ công suất (≥ tổng TDP × 1.3)
            5. Nếu thiếu loại linh kiện nào → nói thẳng "Cửa hàng chưa có [loại] phù hợp".
            6. BẮT BUỘC trả lời hoàn toàn bằng tiếng Việt, tuyệt đối không dùng ngôn ngữ khác (trừ thuật ngữ chuyên ngành).

            CẤU TRÚC TRẢ LỜI:
            Mở đầu: 1 câu tóm tắt mục đích build và ngân sách.

            🖥️ **CẤU HÌNH ĐỀ XUẤT:**

            | Linh kiện | Sản phẩm | Giá |
            |-----------|----------|-----|
            | 🔲 CPU | [Tên] | [Giá từ Tool] |
            | 🔲 Mainboard | [Tên] | [Giá từ Tool] |
            | 🔲 RAM | [Tên] | [Giá từ Tool] |
            | 🎮 VGA | [Tên] | [Giá từ Tool] |
            | 💾 SSD | [Tên] | [Giá từ Tool] |
            | ⚡ Nguồn | [Tên] | [Giá từ Tool] |
            | 🏠 Case | [Tên] | [Giá từ Tool] |
            | ❄️ Tản nhiệt | [Tên] | [Giá từ Tool] |

            💰 **Tổng cộng: [Tổng giá]**

            📝 **Ghi chú tương thích:** [Lưu ý quan trọng về tương thích]
            💡 **Nhận xét:** [Đánh giá ngắn về hiệu năng build này]

            Nhắc người dùng nhấn nút "+" bên cạnh sản phẩm để thêm vào cấu hình.

            CATALOG LINH KIỆN:
            {catalog}
            """;

    public BuildConfigHandler(ChatModel chatModel,
                              VectorStore vectorStore,
                              ProductRepository productRepository,
                              ProductService productService,
                              ObjectMapper objectMapper,
                              CatalogBuilder catalogBuilder) {
        this.chatModel = chatModel;
        this.vectorStore = vectorStore;
        this.productRepository = productRepository;
        this.productService = productService;
        this.objectMapper = objectMapper;
        this.catalogBuilder = catalogBuilder;
    }

    @Override
    public ChatResponse handle(String message, List<ChatMessageDto> history) {
        // 1. Parse price constraints (budget)
        PriceUtils.PriceConstraint constraint = PriceUtils.parsePriceConstraints(message);

        // 2. Clean price keywords
        String queryMessage = PriceUtils.cleanPriceKeywords(message);
        if (queryMessage.isBlank()) queryMessage = message;

        // 3. Wide vector search
        List<Document> relevantDocs;
        try {
            SearchRequest searchRequest = SearchRequest.builder()
                    .query(queryMessage)
                    .topK(TOP_K)
                    .similarityThreshold(SIMILARITY_THRESHOLD)
                    .build();

            List<Document> rawDocs = vectorStore.similaritySearch(searchRequest);
            System.out.printf("[BuildConfig] Query='%s' | rawDocs=%d%n", queryMessage, rawDocs.size());

            // 4. Filter theo giá nếu có budget
            List<Document> filteredDocs = catalogBuilder.filterByPrice(rawDocs, constraint);

            // 5. Group theo category, loại bỏ laptop/PC nguyên bộ/gear
            relevantDocs = groupByCategory(filteredDocs);
            System.out.printf("[BuildConfig] After grouping: %d docs%n", relevantDocs.size());
        } catch (Exception e) {
            System.err.println("[BuildConfig] Search failed: " + e.getMessage());
            relevantDocs = List.of();
        }

        // 6. Build prompt
        String catalog = catalogBuilder.buildCatalog(relevantDocs);
        String systemContent = SYSTEM_PROMPT.replace("{catalog}", catalog);

        List<Message> messages = buildMessages(systemContent, history, message);

        // 7. Gọi LLM
        String aiResponse = callLlm(messages);

        // 8. Extract recommended products
        List<RecommendedProductDto> recommended = catalogBuilder.extractRecommendedProducts(relevantDocs, aiResponse);

        return new ChatResponse(aiResponse, recommended);
    }

    // ── Private helpers ───────────────────────────────────────

    /**
     * Group documents theo category, loại bỏ categories không phù hợp cho build,
     * lấy tối đa PER_CATEGORY_LIMIT sản phẩm mỗi category.
     */
    private List<Document> groupByCategory(List<Document> docs) {
        Map<String, List<Document>> grouped = new HashMap<>();

        for (Document doc : docs) {
            String catSlug = (String) doc.getMetadata().get("categorySlug");
            if (catSlug == null) continue;

            String cleanCat = catSlug.toLowerCase();

            // Skip excluded categories
            boolean excluded = false;
            for (String excl : EXCLUDED_CATEGORIES) {
                if (cleanCat.contains(excl)) {
                    excluded = true;
                    break;
                }
            }
            if (excluded) continue;

            grouped.computeIfAbsent(cleanCat, k -> new ArrayList<>()).add(doc);
        }

        List<Document> result = new ArrayList<>();
        for (var entry : grouped.entrySet()) {
            List<Document> catDocs = entry.getValue();
            result.addAll(catDocs.subList(0, Math.min(PER_CATEGORY_LIMIT, catDocs.size())));
        }
        return result;
    }

    private List<Message> buildMessages(String systemContent, List<ChatMessageDto> history, String message) {
        List<Message> messages = new ArrayList<>();
        messages.add(new SystemMessage(systemContent));

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
        return messages;
    }

    private String callLlm(List<Message> messages) {
        try {
            var callback = FunctionToolCallback.builder(
                            "getProductDetails",
                            new ProductDetailsFunction(productRepository, productService, objectMapper))
                    .description("Lấy thông tin chi tiết sản phẩm (giá, tồn kho, thông số) theo productId")
                    .inputType(ProductDetailsFunction.Request.class)
                    .build();

            OllamaChatOptions options = OllamaChatOptions.builder()
                    .toolCallbacks(List.of(callback))
                    .build();

            Prompt prompt = new Prompt(messages, options);
            return chatModel.call(prompt).getResult().getOutput().getText();
        } catch (Exception e) {
            System.err.println("[BuildConfig] LLM call failed: " + e.getMessage());
            return "Xin lỗi bạn, mình đang gặp sự cố kết nối. Bạn thử hỏi lại sau nhé! 🙏";
        }
    }
}

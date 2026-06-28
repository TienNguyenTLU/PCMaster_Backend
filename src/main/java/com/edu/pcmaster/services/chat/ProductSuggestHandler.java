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
import java.util.List;

/**
 * Handler cho câu hỏi gợi ý sản phẩm theo giá, thông số, mục đích.
 * Ví dụ: "Gợi ý VGA tầm 10 triệu", "RAM DDR5 nào tốt cho gaming?"
 */
@Component
public class ProductSuggestHandler implements ChatHandler {

    private final ChatModel chatModel;
    private final VectorStore vectorStore;
    private final ProductRepository productRepository;
    private final ProductService productService;
    private final ObjectMapper objectMapper;
    private final RerankerService rerankerService;
    private final CatalogBuilder catalogBuilder;

    private static final int TOP_K = 50;
    private static final double SIMILARITY_THRESHOLD = 0.3;
    private static final double PRICE_SEARCH_THRESHOLD = 0.20;
    private static final int MAX_RESULTS = 5;
    private static final int MAX_HISTORY_TURNS = 6;

    private static final String SYSTEM_PROMPT = """
            Bạn là tư vấn viên nhiệt tình tại cửa hàng linh kiện PCMaster. Nhiệm vụ: gợi ý sản phẩm phù hợp với yêu cầu khách hàng.

            PHONG CÁCH GIAO TIẾP:
            - Xưng "mình", gọi khách là "bạn"
            - Nhiệt tình, chi tiết nhưng không dài dòng
            - Dùng emoji (🌟🎯🛒💡🔥✨) tạo sự thân thiện
            - Giải thích tại sao mỗi sản phẩm phù hợp với nhu cầu

            QUY TẮC BẮT BUỘC:
            1. CHỈ gợi ý sản phẩm có trong CATALOG bên dưới. CẤM bịa tên sản phẩm.
            2. BẮT BUỘC gọi `getProductDetails` cho TỪNG sản phẩm để lấy giá, tồn kho, thông số thực tế.
            3. KHÔNG tự bịa giá, tồn kho, thông số. CHỈ dùng dữ liệu từ tool.
            4. Nếu stock = 0 → thông báo hết hàng, gợi ý sản phẩm khác.
            5. Sắp xếp sản phẩm từ phù hợp nhất đến ít phù hợp hơn.
            6. BẮT BUỘC trả lời hoàn toàn bằng tiếng Việt, tuyệt đối không dùng ngôn ngữ khác (trừ thuật ngữ chuyên ngành).

            CẤU TRÚC TRẢ LỜI:
            Mở đầu bằng 1 câu tóm tắt hiểu yêu cầu của khách.

            Sau đó liệt kê từng sản phẩm:
            🌟 **[Tên sản phẩm] — [Giá từ Tool]**
            🎯 **Phù hợp vì:** [Lý do ngắn gọn tại sao sản phẩm phù hợp yêu cầu]
            🛠️ **Điểm mạnh:**
            - [Thông số/tính năng nổi bật]
            - ...
            🛒 **Tình trạng:** [Còn X sản phẩm]

            Kết thúc bằng lời khuyên tổng quan (nên chọn cái nào nếu...).

            CATALOG SẢN PHẨM:
            {catalog}
            """;

    public ProductSuggestHandler(ChatModel chatModel,
                                 VectorStore vectorStore,
                                 ProductRepository productRepository,
                                 ProductService productService,
                                 ObjectMapper objectMapper,
                                 RerankerService rerankerService,
                                 CatalogBuilder catalogBuilder) {
        this.chatModel = chatModel;
        this.vectorStore = vectorStore;
        this.productRepository = productRepository;
        this.productService = productService;
        this.objectMapper = objectMapper;
        this.rerankerService = rerankerService;
        this.catalogBuilder = catalogBuilder;
    }

    @Override
    public ChatResponse handle(String message, List<ChatMessageDto> history) {
        // 1. Parse price constraints
        PriceUtils.PriceConstraint constraint = PriceUtils.parsePriceConstraints(message);
        boolean hasPriceConstraint = constraint.hasConstraint();

        // 2. Clean price keywords khỏi query để vector search chính xác hơn
        String queryMessage = PriceUtils.cleanPriceKeywords(message);
        if (queryMessage.isBlank()) queryMessage = message;

        // 3. Vector search — dải rộng hơn ProductInfo
        List<Document> relevantDocs;
        try {
            double threshold = hasPriceConstraint ? PRICE_SEARCH_THRESHOLD : SIMILARITY_THRESHOLD;

            SearchRequest.Builder searchBuilder = SearchRequest.builder()
                    .query(queryMessage)
                    .topK(TOP_K)
                    .similarityThreshold(threshold);

            String filterExpr = catalogBuilder.extractFilterExpression(message);
            if (filterExpr != null) {
                searchBuilder.filterExpression(filterExpr);
                System.out.println("[ProductSuggest] Applied filter: " + filterExpr);
            }

            List<Document> rawDocs = vectorStore.similaritySearch(searchBuilder.build());
            System.out.printf("[ProductSuggest] Query='%s' | threshold=%.2f | rawDocs=%d%n",
                    queryMessage, threshold, rawDocs.size());

            // 4. Filter theo giá
            List<Document> filteredDocs = catalogBuilder.filterByPrice(rawDocs, constraint);
            System.out.printf("[ProductSuggest] After price filter: %d docs%n", filteredDocs.size());

            // 5. Rerank và lấy top N
            List<Document> reranked = rerankerService.rerankDocuments(queryMessage, filteredDocs);
            relevantDocs = reranked.subList(0, Math.min(MAX_RESULTS, reranked.size()));
        } catch (Exception e) {
            System.err.println("[ProductSuggest] Search failed: " + e.getMessage());
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
            System.err.println("[ProductSuggest] LLM call failed: " + e.getMessage());
            return "Xin lỗi bạn, mình đang gặp sự cố kết nối. Bạn thử hỏi lại sau nhé! 🙏";
        }
    }
}

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
 * Handler cho câu hỏi thông tin chi tiết về 1 sản phẩm cụ thể.
 * Ví dụ: "RTX 4070 có gì nổi bật?", "Thông số i7-14700K"
 */
@Component
public class ProductInfoHandler implements ChatHandler {

    private final ChatModel chatModel;
    private final VectorStore vectorStore;
    private final ProductRepository productRepository;
    private final ProductService productService;
    private final ObjectMapper objectMapper;
    private final RerankerService rerankerService;
    private final CatalogBuilder catalogBuilder;

    private static final int TOP_K = 5;
    private static final double SIMILARITY_THRESHOLD = 0.35;
    private static final int MAX_HISTORY_TURNS = 6;

    private static final String SYSTEM_PROMPT = """
            Bạn là trợ lý tư vấn thân thiện tại cửa hàng linh kiện PCMaster. Nhiệm vụ: đưa thông tin chi tiết về sản phẩm.

            PHONG CÁCH GIAO TIẾP:
            - Xưng "mình", gọi khách là "bạn"
            - Dùng emoji phù hợp (🌟🎯🛒💡🔥) để tạo sự thân thiện
            - Giải thích thuật ngữ kỹ thuật bằng ngôn ngữ đời thường, dễ hiểu
            - Đưa ra nhận xét khách quan về ưu/nhược điểm

            QUY TẮC BẮT BUỘC:
            1. CHỈ đề xuất sản phẩm có trong CATALOG bên dưới. Tuyệt đối CẤM bịa tên sản phẩm.
            2. BẮT BUỘC gọi `getProductDetails` với `productId` để lấy: giá, tồn kho, thông số kỹ thuật.
            3. KHÔNG tự bịa hoặc suy đoán giá, tồn kho, thông số. CHỈ dùng dữ liệu từ tool trả về.
            4. Nếu stock = 0, thông báo hết hàng và gợi ý sản phẩm tương tự.
            5. BẮT BUỘC trả lời hoàn toàn bằng tiếng Việt, tuyệt đối không dùng ngôn ngữ khác (trừ thuật ngữ chuyên ngành).

            CẤU TRÚC TRẢ LỜI:
            🌟 **[Tên sản phẩm]**
            💰 **Giá:** [Giá từ Tool] (nếu có khuyến mãi thì ghi rõ)

            📋 **Thông số nổi bật:**
            - [Thông số quan trọng nhất, giải thích dễ hiểu]
            - ...

            ✅ **Ưu điểm:** [Liệt kê ngắn gọn]
            ⚠️ **Lưu ý:** [Nếu có điều cần lưu ý]

            🛒 **Tình trạng:** [Còn hàng/Hết hàng] — [Lời khuyên mua hàng]

            CATALOG SẢN PHẨM:
            {catalog}
            """;

    public ProductInfoHandler(ChatModel chatModel,
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
        // 1. Vector search — tìm sản phẩm gần nhất với query
        List<Document> relevantDocs;
        try {
            SearchRequest.Builder searchBuilder = SearchRequest.builder()
                    .query(message)
                    .topK(TOP_K)
                    .similarityThreshold(SIMILARITY_THRESHOLD);

            String filterExpr = catalogBuilder.extractFilterExpression(message);
            if (filterExpr != null) {
                searchBuilder.filterExpression(filterExpr);
                System.out.println("[ProductInfo] Applied filter: " + filterExpr);
            }

            List<Document> rawDocs = vectorStore.similaritySearch(searchBuilder.build());
            System.out.printf("[ProductInfo] Query='%s' | rawDocs=%d%n", message, rawDocs.size());

            // Rerank và lấy top 1-2 sản phẩm
            List<Document> reranked = rerankerService.rerankDocuments(message, rawDocs);
            relevantDocs = reranked.subList(0, Math.min(2, reranked.size()));
        } catch (Exception e) {
            System.err.println("[ProductInfo] Search failed: " + e.getMessage());
            relevantDocs = List.of();
        }

        // 2. Build prompt với catalog
        String catalog = catalogBuilder.buildCatalog(relevantDocs);
        String systemContent = SYSTEM_PROMPT.replace("{catalog}", catalog);

        List<Message> messages = buildMessages(systemContent, history, message);

        // 3. Gọi LLM với tool calling
        String aiResponse = callLlm(messages);

        // 4. Extract recommended products
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
            System.err.println("[ProductInfo] LLM call failed: " + e.getMessage());
            return "Xin lỗi bạn, mình đang gặp sự cố kết nối. Bạn thử hỏi lại sau nhé! 🙏";
        }
    }
}

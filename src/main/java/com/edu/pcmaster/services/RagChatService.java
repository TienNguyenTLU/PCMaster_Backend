package com.edu.pcmaster.services;

import com.edu.pcmaster.dto.chatbot.ChatMessageDto;
import com.edu.pcmaster.dto.chatbot.ChatResponse;
import com.edu.pcmaster.dto.chatbot.RecommendedProductDto;
import com.edu.pcmaster.models.Product;
import com.edu.pcmaster.repositories.ProductRepository;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * RagChatService - Trợ lý ảo tư vấn phần cứng và lắp ráp PC Master.
 * Logic được thiết kế tinh giản, tập trung vào kho hàng thực tế của cơ sở dữ liệu.
 */
@Service
public class RagChatService {

    private final ChatModel chatModel;
    private final ProductRepository productRepository;
    private final VectorStore vectorStore;

    private static final int MAX_HISTORY_TURNS = 6;

    private static final String SYSTEM_PROMPT_TEMPLATE = """
            Bạn là trợ lý tư vấn phần cứng máy tính và lắp ráp PC tại PCMaster.
            Nhiệm vụ của bạn là tư vấn nhiệt tình, giải đáp câu hỏi của người dùng và đề xuất cấu hình máy tính phù hợp với nhu cầu và ngân sách.

            QUY TẮC PHÁT NGÔN BẮT BUỘC:
            1. BẮT BUỘC TRẢ LỜI HOÀN TOÀN BẰNG TIẾNG VIỆT CHUẨN. Tuyệt đối không trả lời bằng chữ Trung Quốc hay bất kỳ ngôn ngữ nào khác.
            2. Xưng hô: Xưng "mình", gọi khách hàng là "bạn" và sử dụng các emoji thân thiện (😊, 👋, 💻, 🎮, ⚙️).
            3. GIỚI HẠN KHO HÀNG THỰC TẾ: Bạn chỉ được phép chọn, giới thiệu và tư vấn các sản phẩm có thật trong danh sách dưới đây. Tuyệt đối KHÔNG tự chế tên linh kiện, giá bán hay thông số kỹ thuật.
            4. Khi tư vấn cấu hình PC:
               - Kiểm tra tương thích cơ bản (CPU Socket phải khớp với Mainboard Socket; RAM DDR4 hay DDR5 phải tương thích với Mainboard).
               - Tính toán công suất nguồn (PSU) lớn hơn tổng TDP ước lượng của CPU + GPU.
               - Cung cấp bảng/danh sách báo giá chi tiết và tính tổng cộng chi phí. Đảm bảo tổng giá tiền phù hợp với ngân sách của khách hàng (chênh lệch tối đa 5%).
            5. BẮT BUỘC kết thúc phản hồi bằng một dòng chứa các ID sản phẩm đã đề xuất theo cú pháp chính xác sau:
               RECOMMENDED_IDS: [id1, id2, id3, ...]
               Ví dụ: RECOMMENDED_IDS: [12, 25, 54, 167]
               Nếu không đề xuất sản phẩm nào từ kho hàng, hãy viết: RECOMMENDED_IDS: []

            DANH SÁCH LINH KIỆN CÓ SẴN TRONG KHO (BẮT BUỘC CHỌN TỪ ĐÂY):
            {catalog}
            """;

    public RagChatService(ChatModel chatModel, ProductRepository productRepository, VectorStore vectorStore) {
        this.chatModel = chatModel;
        this.productRepository = productRepository;
        this.vectorStore = vectorStore;
    }

    /**
     * Entry point cho Chatbot.
     */
    @Transactional(readOnly = true)
    public ChatResponse chat(String message, List<ChatMessageDto> history, String mode) {
        // 1. Lấy danh sách sản phẩm thực tế từ Database (loại bỏ các sản phẩm test)
        List<Product> allProducts = productRepository.findAll();
        List<Product> activeProducts = allProducts.stream()
                .filter(p -> p.getStock() > 0)
                .filter(p -> p.getName() != null && !p.getName().toLowerCase().contains("test"))
                .filter(p -> p.getSlug() != null && !p.getSlug().toLowerCase().contains("test"))
                .toList();

        // 2. Chọn linh kiện thông minh để đưa vào prompt (tránh vượt giới hạn context 4096 tokens)
        Set<Product> selectedProducts = new LinkedHashSet<>();

        // 2a. Vector Search để lấy các sản phẩm khớp với câu hỏi người dùng
        try {
            List<Document> docHits = vectorStore.similaritySearch(
                    SearchRequest.builder()
                            .query(message)
                            .topK(3)
                            .similarityThreshold(0.8)
                            .build()
            );
            for (Document doc : docHits) {
                Object idObj = doc.getMetadata().get("productId");
                if (idObj != null) {
                    try {
                        Long id = Long.valueOf(idObj.toString());
                        activeProducts.stream().filter(p -> p.getId().equals(id)).findFirst().ifPresent(selectedProducts::add);
                    } catch (Exception ignored) {}
                }
            }
        } catch (Exception e) {
            System.err.println("[RagChatService] Vector search failed: " + e.getMessage());
        }

        // 2b. Lấy ma trận các linh kiện đại diện cho từng danh mục để build cấu hình (cheapest, mid, premium)
        Map<String, List<Product>> grouped = activeProducts.stream()
                .filter(p -> getComponentType(p) != null)
                .collect(Collectors.groupingBy(this::getComponentType));

        for (Map.Entry<String, List<Product>> entry : grouped.entrySet()) {
            String type = entry.getKey();
            if (type == null) continue; // Bỏ qua sản phẩm không phân loại được
            List<Product> list = entry.getValue();
            list.sort(Comparator.comparing(Product::getPrice));

            int size = list.size();
            if (size > 0) selectedProducts.add(list.get(0)); // cheapest
            if (size > 2) selectedProducts.add(list.get(size / 3)); // mid-low
            if (size > 3) selectedProducts.add(list.get(2 * size / 3)); // mid-high
            if (size > 1) selectedProducts.add(list.get(size - 1)); // premium
        }

        // Convert set to list
        List<Product> promptCatalog = new ArrayList<>(selectedProducts);

        // 3. Xây dựng catalog text
        String catalogText = buildCatalogText(promptCatalog);

        // 4. Chuẩn bị System Prompt
        String systemPrompt = SYSTEM_PROMPT_TEMPLATE.replace("{catalog}", catalogText);

        // 5. Chuẩn bị luồng tin nhắn bao gồm lịch sử
        List<Message> messages = new ArrayList<>();
        messages.add(new SystemMessage(systemPrompt));

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

        // 6. Gọi LLM
        String aiResponse;
        try {
            Prompt prompt = new Prompt(messages);
            aiResponse = chatModel.call(prompt).getResult().getOutput().getText();
        } catch (Exception e) {
            System.err.println("[RagChatService] LLM call failed: " + e.getMessage());
            aiResponse = "Chào bạn! 👋 Mình là trợ lý ảo của PCMaster. Hiện tại máy chủ AI của mình đang bận phản hồi, bạn vui lòng gửi lại câu hỏi nhé! 😊";
        }

        // 7. Trích xuất các sản phẩm được đề xuất
        List<RecommendedProductDto> recommendedProducts = extractRecommendedProducts(aiResponse, activeProducts);

        // 8. Làm sạch dòng RECOMMENDED_IDS khỏi phản hồi hiển thị cho người dùng (nếu có)
        String cleanMessage = aiResponse;
        int recommendedIdx = cleanMessage.lastIndexOf("RECOMMENDED_IDS:");
        if (recommendedIdx != -1) {
            cleanMessage = cleanMessage.substring(0, recommendedIdx).trim();
        }

        return new ChatResponse(cleanMessage, recommendedProducts);
    }

    /**
     * Phân loại linh kiện theo category name hoặc slug
     */
    private String getComponentType(Product p) {
        if (p.getCategory() == null) return null;
        String catName = p.getCategory().getName().toLowerCase();
        String catSlug = p.getCategory().getSlug().toLowerCase();

        if (catName.contains("cpu") || catName.contains("vi xử lý") || catSlug.contains("cpu")) return "CPU";
        if (catName.contains("mainboard") || catName.contains("bo mạch chủ") || catSlug.contains("mainboard")) return "MAINBOARD";
        if (catName.contains("ram") || catSlug.contains("ram")) return "RAM";
        if (catName.contains("card màn hình") || catName.contains("vga") || catName.contains("gpu") || catSlug.contains("vga") || catSlug.contains("gpu")) return "GPU";
        if (catName.contains("ổ cứng") || catName.contains("ssd") || catName.contains("hdd") || catSlug.contains("ssd") || catSlug.contains("sata") || catSlug.contains("storage")) return "STORAGE";
        if (catName.contains("nguồn") || catName.contains("psu") || catSlug.contains("psu")) return "PSU";
        if (catName.contains("vỏ máy") || catName.contains("case") || catSlug.contains("atx")) return "CASE";
        if (catName.contains("tản nhiệt") || catName.contains("cooler") || catSlug.contains("cooler")) return "COOLER";

        return null;
    }

    /**
     * Định dạng kho hàng thành chuỗi văn bản cho LLM đọc.
     */
    private String buildCatalogText(List<Product> products) {
        StringBuilder sb = new StringBuilder();
        
        Map<String, List<Product>> grouped = products.stream()
                .filter(p -> p.getCategory() != null)
                .collect(Collectors.groupingBy(p -> p.getCategory().getName()));

        for (Map.Entry<String, List<Product>> entry : grouped.entrySet()) {
            sb.append("=== DANH MỤC: ").append(entry.getKey().toUpperCase()).append(" ===\n");
            for (Product p : entry.getValue()) {
                sb.append(String.format("  * ID: %d | Tên: %s | Giá: %,.0f VND | Specs: %s\n",
                        p.getId(),
                        p.getName(),
                        p.getPrice().doubleValue(),
                        p.getSpecsJson() != null ? p.getSpecsJson().toString() : "{}"
                ));
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    /**
     * Trích xuất RecommendedProductDto dựa trên ID list được xuất ra bởi LLM hoặc tìm kiếm văn bản thô.
     */
    private List<RecommendedProductDto> extractRecommendedProducts(String responseText, List<Product> catalog) {
        List<RecommendedProductDto> recommended = new ArrayList<>();
        java.util.Set<Long> addedIds = new java.util.HashSet<>();

        // Cách 1: Tìm dòng RECOMMENDED_IDS: [...] ở cuối
        Pattern idPattern = Pattern.compile("RECOMMENDED_IDS:\\s*\\[([0-9\\s,]*)\\]", Pattern.CASE_INSENSITIVE);
        Matcher matcher = idPattern.matcher(responseText);
        if (matcher.find()) {
            String idsStr = matcher.group(1);
            if (!idsStr.isBlank()) {
                String[] parts = idsStr.split("[,\\s]+");
                for (String part : parts) {
                    try {
                        Long id = Long.parseLong(part.trim());
                        if (addedIds.add(id)) {
                            catalog.stream().filter(p -> p.getId().equals(id)).findFirst().ifPresent(p -> {
                                recommended.add(mapToDto(p));
                            });
                        }
                    } catch (NumberFormatException ignored) {}
                }
            }
        }

        // Cách 2: Quét thô tên sản phẩm và slug trong phản hồi nếu danh sách đề xuất trống
        if (recommended.isEmpty()) {
            String responseLower = responseText.toLowerCase();
            for (Product p : catalog) {
                if (addedIds.contains(p.getId())) continue;
                String nameLower = p.getName().toLowerCase();
                String slugClean = p.getSlug() != null ? p.getSlug().replace("_", " ").toLowerCase() : "";

                if (responseLower.contains(nameLower) || (!slugClean.isEmpty() && responseLower.contains(slugClean))) {
                    if (addedIds.add(p.getId())) {
                        recommended.add(mapToDto(p));
                    }
                }
            }
        }

        return recommended;
    }

    /**
     * Map entity Product sang RecommendedProductDto
     */
    private RecommendedProductDto mapToDto(Product p) {
        String categorySlug = p.getCategory() != null ? p.getCategory().getSlug() : "";
        return new RecommendedProductDto(
                p.getId(),
                p.getName(),
                p.getSlug(),
                p.getPrice(),
                null,
                null,
                p.getThumbnailUrl(),
                p.getStock(),
                categorySlug
        );
    }
}

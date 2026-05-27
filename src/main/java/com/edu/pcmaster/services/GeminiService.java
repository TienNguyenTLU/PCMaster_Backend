package com.edu.pcmaster.services;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.edu.pcmaster.dto.chatbot.ChatMessageDto;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Dịch vụ GeminiService kết nối trực tiếp tới Google Gemini 2.5 Flash REST API (Google AI Studio).
 * Chịu trách nhiệm phân tích ngôn ngữ tự nhiên thành JSON truy vấn và sinh lời tư vấn Markdown tiếng Việt.
 * 
 * LƯU Ý QUAN TRỌNG VỀ API VERSION:
 * - Phải sử dụng endpoint "v1beta" (KHÔNG PHẢI "v1") vì chỉ v1beta mới hỗ trợ trường "system_instruction".
 * - Endpoint v1 ổn định nhưng KHÔNG chấp nhận "system_instruction" hay "systemInstruction" → gây lỗi 400.
 * - Tên trường JSON: "system_instruction" (snake_case), "generationConfig" (camelCase), "contents", "parts".
 */
@Service
public class GeminiService {

    // Khóa API Key của Google AI Studio (được nạp từ application.properties)
    @Value("${gemini.api.key}")
    private String apiKey;

    // Đường dẫn REST API chính thức của mô hình Gemini generateContent
    @Value("${gemini.api.url}")
    private String apiUrl;

    // RestTemplate để thực hiện gọi HTTP request đồng bộ sang máy chủ Google
    private final RestTemplate restTemplate;
    
    // ObjectMapper của thư viện Jackson để parse/đóng gói JSON
    private final ObjectMapper objectMapper;

    /**
     * Constructor tiêm phụ thuộc ObjectMapper và khởi tạo RestTemplate.
     */
    public GeminiService(ObjectMapper objectMapper) {
        this.restTemplate = new RestTemplate();
        this.objectMapper = objectMapper;
    }

    /**
     * DTO đại diện cho ý định lọc sản phẩm (Intent) được AI bóc tách từ câu hỏi của khách.
     * categorySlug: danh mục sản phẩm (vga, cpu, ram...)
     * brandSlug: thương hiệu (asus, msi...)
     * maxPrice: giá tiền tối đa (ngân sách)
     * keyword: từ khóa tự do tìm kiếm trong tên/mô tả
     */
    public record ChatbotIntent(
        String categorySlug,
        String brandSlug,
        java.math.BigDecimal maxPrice,
        String keyword
    ) {}

    /**
     * BƯỚC 1: DỊCH Ý ĐỊNH (INTENT PARSING)
     * Đọc tin nhắn ngôn ngữ tự nhiên của khách và dùng Gemini để dịch thành định dạng JSON bộ lọc.
     * Sử dụng tính năng JSON Mode (responseMimeType: application/json) để đảm bảo đầu ra luôn là JSON hợp lệ.
     */
    public ChatbotIntent parseIntent(String message, List<ChatMessageDto> history) {
        try {
            // Kiểm tra an toàn phòng trường hợp API key chưa được cấu hình
            if (apiKey == null || apiKey.isBlank()) {
                return new ChatbotIntent(null, null, null, message);
            }

            // Đường dẫn API kèm Key xác thực ở URL Query Parameter
            String url = apiUrl + "?key=" + apiKey;

            // Thiết lập Headers bắt buộc cho POST Request
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            // Hệ thống Prompt (System Instruction) chỉ thị rõ ràng hành vi ép trả về JSON cấu trúc
            String systemInstruction = "You are a strict JSON generator. Your task is to analyze the user's message and extract their shopping intent for computer hardware or PC builds into a valid JSON object. You must output ONLY a valid JSON object, no markdown blocks, no triple backticks, and no extra text.\n"
                    + "The JSON format MUST be exactly:\n"
                    + "{\n"
                    + "  \"categorySlug\": \"slug_or_null\",\n"
                    + "  \"brandSlug\": \"slug_or_null\",\n"
                    + "  \"maxPrice\": number_or_null,\n"
                    + "  \"keyword\": \"string_or_null\"\n"
                    + "}\n"
                    + "Common category slugs: 'cpu', 'gpu' (for graphics cards, VGA), 'ram', 'mainboard', 'ssd', 'psu' (for power supplies), 'case', 'pc-system' (for pre-built PCs), 'monitor'.\n"
                    + "Common brand slugs: 'asus', 'msi', 'gigabyte', 'intel', 'amd', 'nvidia', 'corsair', 'kingston', 'samsung'.\n"
                    + "If the user does not specify a field, set it to null.";

            // 1. Tạo Request Body dưới dạng Node Tree của Jackson
            ObjectNode requestBody = objectMapper.createObjectNode();
            
            // 2. Thiết lập nội dung hội thoại (contents)
            ArrayNode contentsNode = requestBody.putArray("contents");
            ObjectNode userContent = contentsNode.addObject();
            userContent.put("role", "user");
            ArrayNode partsNode = userContent.putArray("parts");
            partsNode.addObject().put("text", message);

            // 3. Thiết lập System Instruction (Hướng dẫn hệ thống định hình hành vi AI)
            ObjectNode systemNode = requestBody.putObject("system_instruction");
            ArrayNode sysParts = systemNode.putArray("parts");
            sysParts.addObject().put("text", systemInstruction);

            // 4. Cấu hình JSON Mode ép buộc model trả về đối tượng JSON thuần túy
            ObjectNode genConfig = requestBody.putObject("generationConfig");
            genConfig.put("responseMimeType", "application/json");
            genConfig.put("maxOutputTokens", 300); // Giới hạn token đầu ra ngắn gọn

            // Đóng gói HTTP Entity và thực thi lệnh POST qua mạng
            HttpEntity<String> entity = new HttpEntity<>(requestBody.toString(), headers);
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);

            // Xử lý kết quả trả về khi gọi thành công
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());
                String jsonText = root.path("candidates")
                        .path(0)
                        .path("content")
                        .path("parts")
                        .path(0)
                        .path("text")
                        .asText();

                // Đọc văn bản JSON AI trả về để ánh xạ sang đối tượng DTO ChatbotIntent
                JsonNode parsedJson = objectMapper.readTree(jsonText.trim());
                
                String categorySlug = parsedJson.path("categorySlug").isNull() ? null : parsedJson.path("categorySlug").asText();
                String brandSlug = parsedJson.path("brandSlug").isNull() ? null : parsedJson.path("brandSlug").asText();
                java.math.BigDecimal maxPrice = null;
                if (parsedJson.has("maxPrice") && !parsedJson.path("maxPrice").isNull()) {
                    maxPrice = new java.math.BigDecimal(parsedJson.path("maxPrice").asText());
                }
                String keyword = parsedJson.path("keyword").isNull() ? null : parsedJson.path("keyword").asText();

                return new ChatbotIntent(
                    "null".equalsIgnoreCase(categorySlug) ? null : categorySlug,
                    "null".equalsIgnoreCase(brandSlug) ? null : brandSlug,
                    maxPrice,
                    "null".equalsIgnoreCase(keyword) ? null : keyword
                );
            }
        } catch (Exception ex) {
            // Ghi nhận log lỗi nếu xảy ra sự cố gọi mạng hoặc parse dữ liệu nhưng không làm crash ứng dụng
            System.err.println("Error parsing intent with Gemini: " + ex.getMessage());
        }

        // Dự phòng: Trả về bộ lọc cơ bản nếu gặp sự cố kết nối AI
        return new ChatbotIntent(null, null, null, message);
    }

    /**
     * BƯỚC 3: TỔNG HỢP CÂU TRẢ LỜI TƯ VẤN (RESPONSE GENERATION)
     * Nhận câu hỏi gốc, lịch sử chat và dữ liệu hàng thật từ DB để viết lời thoại tư vấn sinh động bằng Markdown.
     */
    public String generateResponse(String userMessage, List<ChatMessageDto> history, String databaseProductsContext) {
        try {
            if (apiKey == null || apiKey.isBlank()) {
                return "Chào bạn! Hiện tại kết nối đến máy chủ AI của PCMaster đang bận. Tuy nhiên, bạn vẫn có thể tìm kiếm trực tiếp các sản phẩm linh kiện tại thanh tìm kiếm trên trang Explore của chúng tôi!";
            }

            String url = apiUrl + "?key=" + apiKey;

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            // System Instruction quy định văn phong chuyên nghiệp, định hình làm tư vấn viên PCMaster
            String systemInstruction = "Bạn là một chuyên gia tư vấn phần cứng và lắp ráp PC chuyên nghiệp, thân thiện tại PCMaster (Hệ thống PC & Linh kiện chính hãng hàng đầu).\n"
                    + "Nhiệm vụ của bạn là đọc tin nhắn của khách hàng, trò chuyện vui vẻ, tư vấn nhiệt tình và đưa ra gợi ý giải pháp phù hợp.\n"
                    + "Đặc biệt, hãy tham chiếu đến danh sách sản phẩm thực tế đang còn hàng trong kho sau đây để đề xuất chính xác cho khách hàng:\n"
                    + "=== DANH SÁCH SẢN PHẨM THỰC TẾ TRONG KHO ===\n"
                    + databaseProductsContext + "\n"
                    + "=== LƯU Ý KHI TRẢ LỜI ===\n"
                    + "1. Chỉ đề xuất các sản phẩm có tên trong danh sách thực tế ở trên. Tuyệt đối không tự bịa ra sản phẩm hoặc mã linh kiện không tồn tại.\n"
                    + "2. Nếu danh sách sản phẩm trên trống rỗng hoặc không có sản phẩm nào phù hợp ngân sách, hãy lịch sự thông báo và đề xuất họ điều chỉnh khoảng giá hoặc liên hệ Hotline để được hỗ trợ cấu hình tùy biến.\n"
                    + "3. Sử dụng ngôn ngữ tiếng Việt tự nhiên, chuyên nghiệp. Không đề cập đến việc bạn vừa query database, hãy nói tự nhiên như thể bạn nắm rõ kho hàng.\n"
                    + "4. Định dạng câu trả lời bằng Markdown (in đậm, danh sách gạch đầu dòng, hoặc bảng so sánh thông số ngắn gọn) để tăng trải nghiệm đọc.";

            ObjectNode requestBody = objectMapper.createObjectNode();
            ArrayNode contentsNode = requestBody.putArray("contents");

            // 1. Thêm lịch sử trò chuyện (nếu có) vào contents
            // Thực hiện chiến lược chốt chặn an toàn loại bỏ tin nhắn 'model' đứng đầu mảng lịch sử trò chuyện
            if (history != null && !history.isEmpty()) {
                boolean firstUserFound = false;
                for (ChatMessageDto msg : history) {
                    String role = msg.role();
                    
                    // Bỏ qua tất cả các tin nhắn của trợ lý ảo đứng trước cho đến khi gặp câu hỏi đầu tiên của User
                    if ("model".equalsIgnoreCase(role) && !firstUserFound) {
                        continue;
                    }
                    if ("user".equalsIgnoreCase(role)) {
                        firstUserFound = true;
                    }

                    ObjectNode histContent = contentsNode.addObject();
                    histContent.put("role", role);
                    ArrayNode histParts = histContent.putArray("parts");
                    histParts.addObject().put("text", msg.content());
                }
            }

            // 2. Thêm tin nhắn hiện tại của User vào cuối mảng hội thoại
            ObjectNode userContent = contentsNode.addObject();
            userContent.put("role", "user");
            ArrayNode partsNode = userContent.putArray("parts");
            partsNode.addObject().put("text", userMessage);

            // 3. Thiết lập System Instruction cho request
            ObjectNode systemNode = requestBody.putObject("system_instruction");
            ArrayNode sysParts = systemNode.putArray("parts");
            sysParts.addObject().put("text", systemInstruction);

            // 4. Giới hạn số lượng Token đầu ra để câu trả lời gọn gàng, tránh treo bộ nhớ
            ObjectNode genConfig = requestBody.putObject("generationConfig");
            genConfig.put("maxOutputTokens", 1500);

            HttpEntity<String> entity = new HttpEntity<>(requestBody.toString(), headers);
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());
                return root.path("candidates")
                        .path(0)
                        .path("content")
                        .path("parts")
                        .path(0)
                        .path("text")
                        .asText();
            }
        } catch (Exception ex) {
            System.err.println("Error generating response with Gemini: " + ex.getMessage());
        }

        // Thông điệp dự phòng thân thiện nếu máy chủ Google AI Studio gặp sự cố gián đoạn
        return "Xin lỗi bạn, trợ lý ảo đang gặp một chút sự cố khi kết nối dữ liệu tư vấn. Bạn vui lòng thử lại sau vài giây hoặc đặt câu hỏi ngắn gọn hơn nhé!";
    }
}

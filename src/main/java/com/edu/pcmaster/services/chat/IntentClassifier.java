package com.edu.pcmaster.services.chat;

import com.edu.pcmaster.models.Brand;
import com.edu.pcmaster.models.Category;
import com.edu.pcmaster.repositories.BrandRepository;
import com.edu.pcmaster.repositories.CategoryRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Rule-based intent classifier cho RAG Chat.
 * Phân loại câu hỏi vào 1 trong 4 intent mà không cần gọi LLM.
 */
@Component
public class IntentClassifier {

    private final BrandRepository brandRepository;
    private final CategoryRepository categoryRepository;

    // ── Keyword patterns ──────────────────────────────────────

    /** Từ khóa xây dựng cấu hình PC */
    private static final Set<String> BUILD_KEYWORDS = Set.of(
            "build", "cấu hình", "lắp ráp", "xây dựng", "bộ pc", "bộ máy",
            "ráp pc", "ráp máy", "build pc", "setup pc", "dựng pc",
            "lắp pc", "lắp máy", "combo", "full set", "trọn bộ"
    );

    /** Từ khóa gợi ý / tư vấn sản phẩm */
    private static final Set<String> SUGGEST_KEYWORDS = Set.of(
            "gợi ý", "tư vấn", "đề xuất", "recommend", "nên mua",
            "nên chọn", "mua gì", "chọn gì", "so sánh", "giữa",
            "hay", "hoặc", "top", "best", "tốt nhất"
    );

    /** Từ khóa liên quan đến giá */
    private static final Set<String> PRICE_KEYWORDS = Set.of(
            "giá", "bao nhiêu", "tầm", "khoảng", "dưới", "trên",
            "rẻ", "đắt", "tiết kiệm", "budget", "phải chăng",
            "triệu", "tr", "k", "ngàn", "nghìn"
    );

    /** Từ khóa mục đích sử dụng */
    private static final Set<String> PURPOSE_KEYWORDS = Set.of(
            "gaming", "game", "đồ họa", "render", "văn phòng", "học tập",
            "livestream", "stream", "edit video", "chỉnh ảnh", "lập trình",
            "code", "AI", "machine learning", "deep learning"
    );

    /** Từ khóa hỏi thông tin chi tiết sản phẩm */
    private static final Set<String> INFO_KEYWORDS = Set.of(
            "thông số", "spec", "chi tiết", "review", "đánh giá",
            "có gì", "tính năng", "nổi bật", "khác gì", "như thế nào",
            "ra sao", "hiệu năng", "benchmark", "xung nhịp", "tốc độ"
    );

    /** Từ khóa chào hỏi / hỏi chung */
    private static final Set<String> GREETING_KEYWORDS = Set.of(
            "xin chào", "chào", "hello", "hi", "hey", "helu",
            "cảm ơn", "thank", "thanks", "bye", "tạm biệt",
            "bạn là ai", "giúp gì", "trợ giúp", "help"
    );

    /** Pattern phát hiện model number (e.g., RTX 4070, i7-14700K, RX 7800 XT) */
    private static final Pattern MODEL_NUMBER_PATTERN = Pattern.compile(
            "(?i)(?:" +
            "(?:rtx|gtx|rx|arc)\\s*\\d{3,4}" +                          // GPU models
            "|(?:i[3579]|core\\s*i[3579])[-\\s]?\\d{4,5}" +             // Intel CPU
            "|(?:ryzen\\s*[3579])\\s*\\d{4}" +                          // AMD CPU
            "|(?:b\\d{3}|x\\d{3}|z\\d{3}|h\\d{3}|a\\d{3})" +          // Chipset
            "|(?:ddr[45])\\s*\\d{4}" +                                   // RAM speed
            "|(?:rtx\\s*\\d{4}|geforce|radeon)" +                        // GPU brand
            ")"
    );

    public IntentClassifier(BrandRepository brandRepository,
                            CategoryRepository categoryRepository) {
        this.brandRepository = brandRepository;
        this.categoryRepository = categoryRepository;
    }

    /**
     * Phân loại intent từ message và mode.
     *
     * @param message tin nhắn người dùng
     * @param mode    mode từ frontend ("build" hoặc "consult")
     * @return ChatIntent tương ứng
     */
    public ChatIntent classify(String message, String mode) {
        // Frontend mode override
        if ("build".equalsIgnoreCase(mode)) {
            return ChatIntent.BUILD_CONFIG;
        }

        String lowerMsg = message.toLowerCase().trim();

        // 1. Check greeting / general first (short messages)
        if (isGreeting(lowerMsg)) {
            return ChatIntent.GENERAL;
        }

        // 2. Check build config keywords
        if (containsAny(lowerMsg, BUILD_KEYWORDS)) {
            return ChatIntent.BUILD_CONFIG;
        }

        // 3. Check if asking about a specific product
        boolean hasModelNumber = MODEL_NUMBER_PATTERN.matcher(lowerMsg).find();
        boolean hasSpecificProduct = hasModelNumber || containsSpecificProductName(lowerMsg);

        if (hasSpecificProduct) {
            // If also has suggest/compare keywords → suggest
            if (containsAny(lowerMsg, SUGGEST_KEYWORDS)) {
                return ChatIntent.PRODUCT_SUGGEST;
            }
            // Otherwise → product info
            return ChatIntent.PRODUCT_INFO;
        }

        // 4. Check suggest keywords, price keywords, or purpose keywords
        boolean hasSuggestKw = containsAny(lowerMsg, SUGGEST_KEYWORDS);
        boolean hasPriceKw = containsAny(lowerMsg, PRICE_KEYWORDS);
        boolean hasPurposeKw = containsAny(lowerMsg, PURPOSE_KEYWORDS);
        boolean hasCategoryKw = containsCategoryKeyword(lowerMsg);

        if (hasSuggestKw || hasPurposeKw || (hasPriceKw && hasCategoryKw)) {
            return ChatIntent.PRODUCT_SUGGEST;
        }

        // 5. If has category keyword + info keywords → product info
        if (hasCategoryKw && containsAny(lowerMsg, INFO_KEYWORDS)) {
            return ChatIntent.PRODUCT_INFO;
        }

        // 6. If has any category/brand mention → suggest
        if (hasCategoryKw || containsBrandKeyword(lowerMsg)) {
            return ChatIntent.PRODUCT_SUGGEST;
        }

        // 7. Default → general
        return ChatIntent.GENERAL;
    }

    // ── Helper methods ────────────────────────────────────────

    private boolean isGreeting(String msg) {
        // Very short or just greeting words
        if (msg.length() < 15 && containsAny(msg, GREETING_KEYWORDS)) {
            return true;
        }
        // Exact match greetings
        return GREETING_KEYWORDS.contains(msg);
    }

    private boolean containsAny(String text, Set<String> keywords) {
        for (String kw : keywords) {
            if (text.contains(kw)) return true;
        }
        return false;
    }

    private boolean containsSpecificProductName(String lowerMsg) {
        // Check if the message contains a known brand + model-like pattern
        List<Brand> brands = brandRepository.findAll();
        for (Brand brand : brands) {
            String brandName = brand.getName().toLowerCase();
            if (lowerMsg.contains(brandName)) {
                // If brand name is followed by something that looks like a product name
                int idx = lowerMsg.indexOf(brandName);
                String after = lowerMsg.substring(idx + brandName.length()).trim();
                if (!after.isEmpty() && after.length() > 2) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean containsCategoryKeyword(String lowerMsg) {
        List<Category> categories = categoryRepository.findAll();
        for (Category c : categories) {
            if (lowerMsg.contains(c.getName().toLowerCase())) return true;
            String slug = c.getSlug() != null ? c.getSlug().toLowerCase().replace("-", " ") : "";
            if (!slug.isEmpty() && lowerMsg.contains(slug)) return true;
        }
        // Also check common shorthand
        Set<String> commonCategories = Set.of(
                "cpu", "gpu", "vga", "ram", "ssd", "hdd", "psu", "nguồn",
                "mainboard", "bo mạch chủ", "case", "vỏ máy", "tản nhiệt",
                "cooler", "monitor", "màn hình", "bàn phím", "chuột",
                "keyboard", "mouse", "tai nghe", "headset", "loa", "speaker"
        );
        return containsAny(lowerMsg, commonCategories);
    }

    private boolean containsBrandKeyword(String lowerMsg) {
        List<Brand> brands = brandRepository.findAll();
        for (Brand brand : brands) {
            if (lowerMsg.contains(brand.getName().toLowerCase())) return true;
        }
        return false;
    }
}

package com.edu.pcmaster.services.chat;

import com.edu.pcmaster.dto.chatbot.RecommendedProductDto;
import com.edu.pcmaster.models.Brand;
import com.edu.pcmaster.models.Category;
import com.edu.pcmaster.repositories.BrandRepository;
import com.edu.pcmaster.repositories.CategoryRepository;
import com.edu.pcmaster.repositories.ProductRepository;
import com.edu.pcmaster.services.ProductService;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Utility class cho việc xây dựng catalog text, product DTO,
 * và các helper liên quan đến documents trong RAG Chat.
 */
@Component
public class CatalogBuilder {

    private final ProductRepository productRepository;
    private final ProductService productService;
    private final BrandRepository brandRepository;
    private final CategoryRepository categoryRepository;

    public CatalogBuilder(ProductRepository productRepository,
                          ProductService productService,
                          BrandRepository brandRepository,
                          CategoryRepository categoryRepository) {
        this.productRepository = productRepository;
        this.productService = productService;
        this.brandRepository = brandRepository;
        this.categoryRepository = categoryRepository;
    }

    // ── Build catalog text ────────────────────────────────────

    /**
     * Tạo catalog text từ danh sách documents cho system prompt.
     * Chỉ chứa tên và productId, KHÔNG có giá/tồn kho.
     */
    public String buildCatalog(List<Document> docs) {
        if (docs == null || docs.isEmpty()) {
            return "(Không có sản phẩm khả dụng.)";
        }
        StringBuilder sb = new StringBuilder();
        int idx = 1;
        for (Document doc : docs) {
            Map<String, Object> meta = doc.getMetadata();
            String name = (String) meta.getOrDefault("name", "");
            Object productIdObj = meta.get("productId");
            String productId = productIdObj != null ? productIdObj.toString() : "";
            String category = (String) meta.getOrDefault("category", "");
            String brandName = (String) meta.getOrDefault("brandName", "");

            sb.append(idx++).append(") ").append(name);
            sb.append(" | productId: ").append(productId);
            if (!category.isEmpty()) sb.append(" | Danh mục: ").append(category);
            if (!brandName.isEmpty()) sb.append(" | Thương hiệu: ").append(brandName);
            sb.append("\n");
        }
        sb.append("\n→ Để biết giá, tồn kho, thông số: hãy gọi getProductDetails(productId) cho sản phẩm cần tư vấn.");
        return sb.toString().trim();
    }

    // ── Build product DTO ─────────────────────────────────────

    /**
     * Tạo RecommendedProductDto từ document metadata,
     * lấy dữ liệu real-time từ DB thay vì dùng snapshot cũ.
     */
    public RecommendedProductDto buildProductDto(Map<String, Object> metadata) {
        if (metadata == null) return null;
        try {
            Long id = ((Number) metadata.get("productId")).longValue();

            var productOpt = productRepository.findById(id);
            if (productOpt.isEmpty()) return null;

            var product = productOpt.get();
            Map<Long, Integer> discountsMap = productService.getActiveProductDiscountsMap();
            int discountPct = discountsMap.getOrDefault(id, 0);
            Integer discountPercent = discountPct > 0 ? discountPct : null;
            BigDecimal discountPrice = null;
            if (discountPct > 0) {
                discountPrice = productService.calculateDiscountPrice(product.getPrice(), discountPct);
            }

            String thumbnailUrl = product.getThumbnailUrl();
            if (thumbnailUrl != null && thumbnailUrl.isBlank()) thumbnailUrl = null;

            String categorySlug = product.getCategory() != null && product.getCategory().getSlug() != null
                    ? product.getCategory().getSlug() : "";

            return new RecommendedProductDto(id, product.getName(),
                    product.getSlug() != null ? product.getSlug() : "",
                    product.getPrice(),
                    discountPrice, discountPercent, thumbnailUrl, product.getStock(), categorySlug);
        } catch (Exception e) {
            System.err.println("[RAG] Failed to build product DTO: " + e.getMessage());
            return null;
        }
    }

    // ── Check sản phẩm có được đề cập trong response ─────────

    /**
     * Kiểm tra xem tên sản phẩm có được đề cập trong AI response hay không.
     * Dùng token matching với noise word filtering.
     */
    public boolean isProductMentionedInResponse(String productName, String aiResponseLower) {
        if (productName == null || productName.isBlank()) return false;

        Set<String> noiseWords = Set.of(
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

    // ── Extract metadata filter expression ────────────────────

    /**
     * Tạo filter expression cho vector search dựa trên brand/category
     * được nhắc đến trong message.
     */
    public String extractFilterExpression(String message) {
        try {
            List<Brand> brands = brandRepository.findAll();
            List<Category> categories = categoryRepository.findAll();

            String lowerMessage = message.toLowerCase();

            // Find brand
            String matchedBrand = null;
            for (Brand b : brands) {
                if (lowerMessage.contains(b.getName().toLowerCase())) {
                    matchedBrand = b.getName();
                    break;
                }
            }

            // Find category (longest name first to avoid partial matches)
            List<Category> sortedCategories = new ArrayList<>(categories);
            sortedCategories.sort((c1, c2) -> Integer.compare(c2.getName().length(), c1.getName().length()));

            String matchedCategory = null;
            for (Category c : sortedCategories) {
                if (lowerMessage.contains(c.getName().toLowerCase())) {
                    matchedCategory = c.getName();
                    break;
                }
                String cSlug = c.getSlug() != null ? c.getSlug().toLowerCase().replace("-", " ") : "";
                if (!cSlug.isEmpty() && lowerMessage.contains(cSlug)) {
                    matchedCategory = c.getName();
                    break;
                }
            }

            List<String> conditions = new ArrayList<>();
            if (matchedBrand != null) conditions.add("brand == '" + matchedBrand + "'");
            if (matchedCategory != null) conditions.add("category == '" + matchedCategory + "'");

            if (conditions.isEmpty()) return null;
            return String.join(" && ", conditions);
        } catch (Exception e) {
            System.err.println("[RAG Filter] Error extracting metadata filters: " + e.getMessage());
            return null;
        }
    }

    // ── Filter documents theo giá ────────────────────────────

    /**
     * Lọc documents theo price constraint từ metadata.
     */
    public List<Document> filterByPrice(List<Document> docs, PriceUtils.PriceConstraint constraint) {
        if (!constraint.hasConstraint()) return docs;

        List<Document> filtered = new ArrayList<>();
        for (Document doc : docs) {
            BigDecimal price = extractPrice(doc);
            if (price != null) {
                if (constraint.maxPrice != null && price.compareTo(constraint.maxPrice) > 0) continue;
                if (constraint.minPrice != null && price.compareTo(constraint.minPrice) < 0) continue;
            }
            filtered.add(doc);
        }
        return filtered;
    }

    private BigDecimal extractPrice(Document doc) {
        Map<String, Object> meta = doc.getMetadata();
        // Prefer discount price over original price
        Object discPriceObj = meta.get("discountPrice");
        if (discPriceObj != null) {
            try {
                return new BigDecimal(discPriceObj.toString());
            } catch (Exception ignored) {}
        }
        Object priceObj = meta.get("price");
        if (priceObj != null) {
            try {
                return new BigDecimal(priceObj.toString());
            } catch (Exception ignored) {}
        }
        return null;
    }

    // ── Extract recommended products từ AI response ──────────

    /**
     * Lọc danh sách sản phẩm chỉ giữ những sản phẩm được AI đề cập.
     */
    public List<RecommendedProductDto> extractRecommendedProducts(
            List<Document> relevantDocs, String aiResponse) {

        boolean noProducts = aiResponse.contains("không tìm thấy")
                || aiResponse.contains("không có sản phẩm")
                || aiResponse.contains("chưa có sản phẩm")
                || aiResponse.contains("chưa có linh kiện")
                || aiResponse.contains("sự cố kết nối");

        if (noProducts || relevantDocs.isEmpty()) {
            return List.of();
        }

        String aiLower = aiResponse.toLowerCase();
        return relevantDocs.stream()
                .map(doc -> buildProductDto(doc.getMetadata()))
                .filter(p -> p != null)
                .filter(p -> isProductMentionedInResponse(p.name(), aiLower))
                .toList();
    }
}

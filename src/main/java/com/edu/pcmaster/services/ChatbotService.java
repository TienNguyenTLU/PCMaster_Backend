package com.edu.pcmaster.services;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.edu.pcmaster.dto.brand.BrandResponse;
import com.edu.pcmaster.dto.category.CategoryResponse;
import com.edu.pcmaster.dto.chatbot.ChatMessageDto;
import com.edu.pcmaster.dto.product.ProductResponse;
import com.edu.pcmaster.models.Product;
import com.edu.pcmaster.repositories.ProductRepository;

/**
 * Dịch vụ nghiệp vụ ChatbotService điều phối toàn bộ luồng xử lý của hệ thống Chatbot (Hybrid RAG).
 * Kết hợp thông minh giữa bóc tách ý định bằng AI và truy vấn SQL an toàn trên PostgreSQL.
 */
@Service
public class ChatbotService {

    // Tiêm phụ thuộc (Dependency Injection) các Service và Repository cần thiết
    private final GeminiService geminiService;
    private final ProductRepository productRepository;
    private final ProductService productService;

    /**
     * Constructor tiêm các Beans dịch vụ tương ứng.
     */
    public ChatbotService(GeminiService geminiService, 
                          ProductRepository productRepository, 
                          ProductService productService) {
        this.geminiService = geminiService;
        this.productRepository = productRepository;
        this.productService = productService;
    }

    /**
     * DTO đại diện cho kết cấu phản hồi trả về cho Frontend (Next.js).
     * message: Câu trả lời văn bản Markdown của trợ lý ảo AI.
     * recommendedProducts: Danh sách sản phẩm thực tế còn hàng đề xuất dưới dạng Card.
     */
    public record ChatbotResponse(
        String message,
        List<ProductResponse> recommendedProducts
    ) {}

    /**
     * NGHIỆP VỤ CHÍNH: Xử lý tin nhắn hội thoại tư vấn theo cơ chế Hybrid RAG.
     */
    public ChatbotResponse processChatMessage(String message, List<ChatMessageDto> history) {
        
        // BƯỚC 1: Dùng AI bóc tách ý định người dùng (Intent Parsing) thành JSON lọc
        GeminiService.ChatbotIntent intent = geminiService.parseIntent(message, history);

        // BƯỚC 2: Chuẩn hóa dữ liệu đầu vào & Thực thi SQL nới lỏng lũy tiến (Fallback Strategy)
        List<Product> products = new ArrayList<>();
        String aiProductsContext = ""; // Chuỗi văn bản mô tả kho hàng thật để gửi sang AI

        if (intent != null) {
            String categorySlug = intent.categorySlug();
            String brandSlug = intent.brandSlug();
            java.math.BigDecimal maxPrice = intent.maxPrice();
            String keyword = intent.keyword();

            // =========================================================================
            // MỤC 2.1: BỘ CHUẨN HÓA DANH MỤC (CATEGORY NORMALIZATION)
            // Tác dụng: AI thường bóc tách các từ khóa tiếng Anh/tiếng Việt tự do (như gpu, motherboard, tan_nhiet...).
            // Đoạn mã này có tác dụng chuẩn hóa chúng về đúng chính xác các slug được lưu trong DB của dự án (vga, mainboard, cooler...).
            // Tránh việc bị lệch slug dẫn tới không tìm được sản phẩm nào.
            // =========================================================================
            if (categorySlug != null) {
                categorySlug = categorySlug.toLowerCase().trim().replace("-", "_");
                if ("gpu".equals(categorySlug) || "graphics_card".equals(categorySlug) || "vga_card".equals(categorySlug) || "card_man_hinh".equals(categorySlug) || "card_do_hoa".equals(categorySlug)) {
                    categorySlug = "vga";
                } else if ("processor".equals(categorySlug)) {
                    categorySlug = "cpu";
                } else if ("cooler".equals(categorySlug) || "tan_nhiet".equals(categorySlug) || "fan_cpu".equals(categorySlug)) {
                    categorySlug = "cooler";
                } else if ("main".equals(categorySlug) || "motherboard".equals(categorySlug) || "bo_mach_chu".equals(categorySlug)) {
                    categorySlug = "mainboard";
                } else if ("nguon".equals(categorySlug) || "power_supply".equals(categorySlug)) {
                    categorySlug = "psu";
                } else if ("o_cung".equals(categorySlug) || "hard_drive".equals(categorySlug) || "hdd".equals(categorySlug)) {
                    categorySlug = "ssd";
                } else if ("pc".equals(categorySlug) || "pc_system".equals(categorySlug) || "prebuilt".equals(categorySlug) || "may_bo".equals(categorySlug)) {
                    categorySlug = "pc-system";
                }
            }

            // Chuẩn hóa tên thương hiệu về dạng chữ thường không khoảng trắng đầu cuối
            if (brandSlug != null) {
                brandSlug = brandSlug.toLowerCase().trim();
            }

            // =========================================================================
            // MỤC 2.2: CHIẾN LƯỢC TÌM KIẾM NỚI LỎNG LŨY TIẾN 5 BƯỚC (FALLBACK SEARCH)
            // Tác dụng: Nếu chạy truy vấn AND quá khắt khe, hệ thống dễ bị trả về 0 kết quả (ví dụ: hết hàng hoặc giá quá thấp).
            // Cơ chế này thử tìm chính xác trước. Nếu trống, nó sẽ nới lỏng bộ lọc dần dần để đảm bảo luôn đề xuất được sản phẩm cho khách.
            // =========================================================================
            
            // Bước 2.2.1 (Nghiêm ngặt nhất): Lọc theo đầy đủ các bộ lọc (Danh mục + Hãng + Giá trần + Từ khóa)
            products = productRepository.findProductsForChatbot(categorySlug, brandSlug, maxPrice, keyword);
            String searchStatus = "Chính xác";

            // Bước 2.2.2 (Bỏ từ khóa): Nếu trống, bỏ qua từ khóa tìm kiếm tự do (chỉ giữ Danh mục + Hãng + Giá)
            if (products.isEmpty() && keyword != null && !keyword.isBlank()) {
                products = productRepository.findProductsForChatbot(categorySlug, brandSlug, maxPrice, null);
                searchStatus = "Đề xuất tương tự (Bỏ qua từ khóa)";
            }

            // Bước 2.2.3 (Bỏ giá): Nếu vẫn trống, bỏ qua giới hạn ngân sách để tìm mẫu cùng dòng giá tốt nhất hiện có
            if (products.isEmpty() && maxPrice != null) {
                products = productRepository.findProductsForChatbot(categorySlug, brandSlug, null, null);
                searchStatus = "Đề xuất tương tự (Vượt quá khoảng giá yêu cầu)";
            }

            // Bước 2.2.4 (Bỏ hãng): Nếu vẫn trống, bỏ qua hãng sản xuất để tìm sản phẩm hãng khác cùng loại (ví dụ: thay MSI bằng ASUS)
            if (products.isEmpty() && categorySlug != null) {
                products = productRepository.findProductsForChatbot(categorySlug, null, null, null);
                searchStatus = "Đề xuất tương tự (Hãng khác cùng danh mục)";
            }

            // Bước 2.2.5 (Dự phòng cuối): Nếu kho trống hoàn toàn với tiêu chí trên, gợi ý 8 sản phẩm bất kỳ đang có sẵn
            if (products.isEmpty()) {
                products = productRepository.findProductsForChatbot(null, null, null, null);
                searchStatus = "Đề xuất ngẫu nhiên (Không tìm thấy sản phẩm nào phù hợp tiêu chí)";
            }

            // =========================================================================
            // MỤC 2.3: BỘ CHỈ DẪN NGỮ CẢNH (SEARCH STATUS CONTEXT)
            // Tác dụng: Nhãn `searchStatus` giúp chỉ dẫn hành vi cho AI:
            // - Nếu là "Chính xác" -> AI biết đây là sản phẩm đúng 100%, tự tin liệt kê trực tiếp.
            // - Nếu là "Đề xuất tương tự" -> AI biết đây là hàng thay thế, khéo léo giới thiệu để thuyết phục khách mua.
            // =========================================================================
            StringBuilder contextBuilder = new StringBuilder();
            contextBuilder.append(String.format("=== TRẠNG THÁI TÌM KIẾM DB: %s ===\n", searchStatus));
            if ("Chính xác".equals(searchStatus)) {
                contextBuilder.append("HƯỚNG DẪN: Các sản phẩm bên dưới KHỚP CHÍNH XÁC 100% với yêu cầu của khách hàng. Hãy LIỆT KÊ TRỰC TIẾP và mô tả chi tiết thông số để khách hàng lựa chọn, KHÔNG giới thiệu đây là đề cử thay thế hay tương tự.\n");
            } else {
                contextBuilder.append("HƯỚNG DẪN: Không có sản phẩm nào khớp chính xác. Đây là các sản phẩm ĐỀ XUẤT TƯƠNG TỰ hoặc có thông số gần giống. Hãy giải thích KHÉO LÉO với khách hàng rằng các mẫu chính xác họ tìm hiện không còn hàng hoặc vượt quá ngân sách, và tư vấn họ tham khảo các lựa chọn đề xuất thay thế tuyệt vời dưới đây.\n");
            }

            // Tạo danh sách mô tả thông tin sản phẩm có trong kho
            if (products.isEmpty()) {
                contextBuilder.append("Không có sản phẩm nào phù hợp hoặc còn hàng trong kho tại thời điểm này.");
            } else {
                Map<Long, Integer> discountsMap = productService.getActiveProductDiscountsMap();
                for (Product p : products) {
                    // Tính toán giá tiền thực tế sau khi áp dụng khuyến mãi
                    Integer discountPercent = discountsMap.get(p.getId());
                    BigDecimal discountPrice = productService.calculateDiscountPrice(p.getPrice(), discountPercent);
                    BigDecimal finalPrice = discountPrice != null ? discountPrice : p.getPrice();

                    contextBuilder.append(String.format("- ID: %d, Tên: %s, Slug: %s, Giá gốc: %s, Giá hiện tại: %s, Tồn kho: %d, Mô tả: %s\n",
                        p.getId(),
                        p.getName(),
                        p.getSlug(),
                        p.getPrice().toPlainString() + " VND",
                        finalPrice.toPlainString() + " VND",
                        p.getStock(),
                        p.getDescription() != null ? (p.getDescription().length() > 80 ? p.getDescription().substring(0, 80) + "..." : p.getDescription()) : "Chưa có mô tả"
                    ));
                }
            }
            
            aiProductsContext = contextBuilder.toString();
        }

        // BƯỚC 3: Gửi câu hỏi kèm mô tả hàng thật sang Gemini để viết lời thoại tư vấn sinh động
        String aiResponse = geminiService.generateResponse(message, history, aiProductsContext);

        // BƯỚC 4: Ánh xạ thực thể sản phẩm tìm được sang DTOs để trả về cho Frontend hiển thị Card
        List<ProductResponse> recommendedResponse = new ArrayList<>();
        Map<Long, Integer> discountsMap = productService.getActiveProductDiscountsMap();
        for (Product product : products) {
            recommendedResponse.add(toResponse(product, discountsMap));
        }

        // BƯỚC 5: Đóng gói và trả phản hồi thành công
        return new ChatbotResponse(aiResponse, recommendedResponse);
    }

    /**
     * MỤC 4: MAPPING THỰC THỂ SANG DTO CHUẨN (ENTITY TO DTO MAPPER)
     * Tác dụng: Chuyển đổi đối tượng Hibernate Entity Product sang cấu trúc DTO ProductResponse.
     * Đảm bảo tính toán đúng phần trăm giảm giá, giá khuyến mại và cấu hình linh kiện lắp ghép PC PC_SYSTEM
     * tương thích 100% với giao diện Frontend hiện tại.
     */
    private ProductResponse toResponse(Product product, Map<Long, Integer> discountsMap) {
        List<ProductResponse.PcComponentResponse> pcComponents = null;
        if (product.getPcSystemDetail() != null && product.getPcSystemDetail().getComponents() != null) {
            pcComponents = product.getPcSystemDetail().getComponents().stream()
                    .map(comp -> {
                        Product componentProduct = comp.getComponentProduct();
                        Integer compDiscountPercent = discountsMap.get(componentProduct.getId());
                        BigDecimal compDiscountPrice = productService.calculateDiscountPrice(componentProduct.getPrice(), compDiscountPercent);
                        return new ProductResponse.PcComponentResponse(
                                componentProduct.getId(),
                                componentProduct.getName(),
                                componentProduct.getThumbnailUrl(),
                                compDiscountPrice != null ? compDiscountPrice : componentProduct.getPrice(),
                                comp.getQuantity()
                        );
                    })
                    .toList();
        }
        
        Integer discountPercent = discountsMap.get(product.getId());
        BigDecimal discountPrice = productService.calculateDiscountPrice(product.getPrice(), discountPercent);

        return new ProductResponse(
                product.getId(),
                product.getCategory() == null ? null : product.getCategory().getId(),
                product.getBrand() == null ? null : product.getBrand().getId(),
                toCategoryResponse(product),
                toBrandResponse(product),
                product.getName(),
                product.getSlug(),
                product.getPrice(),
                discountPrice,
                discountPercent,
                product.getStock(),
                product.getThumbnailUrl(),
                product.getDescription(),
                product.getSpecsJson() == null ? null : product.getSpecsJson().toString(),
                product.getCreatedAt(),
                product.getUpdatedAt(),
                pcComponents
        );
    }

    private CategoryResponse toCategoryResponse(Product product) {
        if (product.getCategory() == null) {
            return null;
        }
        return new CategoryResponse(
                product.getCategory().getId(),
                product.getCategory().getName(),
                product.getCategory().getSlug(),
                product.getCategory().getParent() == null ? null : product.getCategory().getParent().getId()
        );
    }

    private BrandResponse toBrandResponse(Product product) {
        if (product.getBrand() == null) {
            return null;
        }
        return new BrandResponse(
                product.getBrand().getId(),
                product.getBrand().getName(),
                product.getBrand().getLogoUrl()
        );
    }
}

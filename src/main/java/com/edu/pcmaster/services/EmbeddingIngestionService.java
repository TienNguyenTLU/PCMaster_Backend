package com.edu.pcmaster.services;

import com.edu.pcmaster.models.Brand;
import com.edu.pcmaster.models.Category;
import com.edu.pcmaster.models.Product;
import com.edu.pcmaster.repositories.ProductRepository;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service ETL (Extract-Transform-Load) chịu trách nhiệm:
 * 1. Đọc toàn bộ sản phẩm từ PostgreSQL
 * 2. Chuyển đổi thành văn bản ngữ nghĩa phong phú (text document)
 * 3. Tạo vector embedding qua Ollama (nomic-embed-text)
 * 4. Lưu vào PGVector store để sẵn sàng tìm kiếm ngữ nghĩa
 *
 * Được kích hoạt bởi Admin qua endpoint: POST /api/admin/chatbot/reindex
 */
@Service
public class EmbeddingIngestionService {

    private final VectorStore vectorStore;
    private final ProductRepository productRepository;
    private final ProductService productService;

    // Số lượng document tối đa trong mỗi batch gửi cho Ollama để embed
    private static final int BATCH_SIZE = 50;

    public EmbeddingIngestionService(VectorStore vectorStore,
                                     ProductRepository productRepository,
                                     ProductService productService) {
        this.vectorStore = vectorStore;
        this.productRepository = productRepository;
        this.productService = productService;
    }

    /**
     * Reindex toàn bộ catalog sản phẩm vào PGVector store.
     * Xóa embedding cũ → tạo embedding mới cho tất cả sản phẩm còn hàng.
     *
     * @return Số lượng sản phẩm đã được embed thành công
     */
    public int reindexAll() {
        List<Product> allProducts = productRepository.findAll();

        // Bước 1: Xóa tất cả embedding cũ theo ID xác định trước
        List<String> existingDocIds = allProducts.stream()
                .map(p -> "product-" + p.getId())
                .collect(Collectors.toList());

        if (!existingDocIds.isEmpty()) {
            try {
                vectorStore.delete(existingDocIds);
            } catch (Exception e) {
                // Bỏ qua lỗi xóa (document có thể chưa tồn tại lần đầu reindex)
                System.out.println("[RAG] Note: Delete existing embeddings returned: " + e.getMessage());
            }
        }

        // Bước 2: Lấy bản đồ khuyến mãi hiện tại
        Map<Long, Integer> discountsMap = productService.getActiveProductDiscountsMap();

        // Bước 3: Tạo Document cho từng sản phẩm còn hàng
        List<Document> documents = allProducts.stream()
                .filter(p -> p.getStock() > 0) // Chỉ index sản phẩm còn tồn kho
                .map(p -> buildDocument(p, discountsMap))
                .collect(Collectors.toList());

        // Bước 4: Gửi cho Ollama để tạo embedding theo lô (batch)
        if (!documents.isEmpty()) {
            for (int i = 0; i < documents.size(); i += BATCH_SIZE) {
                List<Document> batch = documents.subList(i, Math.min(i + BATCH_SIZE, documents.size()));
                vectorStore.add(batch);
                System.out.printf("[RAG] Embedded batch %d/%d (%d documents)%n",
                        (i / BATCH_SIZE) + 1,
                        (int) Math.ceil((double) documents.size() / BATCH_SIZE),
                        batch.size());
            }
        }

        System.out.printf("[RAG] Reindex complete: %d products embedded into PGVector.%n", documents.size());
        return documents.size();
    }

    /**
     * Xây dựng đối tượng Document từ một Product.
     * Text content: mô tả ngữ nghĩa đầy đủ để embedding.
     * Metadata: các trường cấu trúc để tái tạo RecommendedProductDto.
     */
    private Document buildDocument(Product product, Map<Long, Integer> discountsMap) {
        StringBuilder content = new StringBuilder();

        Category category = product.getCategory();
        Brand brand = product.getBrand();

        // Phần nội dung văn bản để embedding ngữ nghĩa
        content.append("Tên sản phẩm: ").append(product.getName()).append("\n");

        if (category != null) {
            content.append("Danh mục: ").append(category.getName());
            if (category.getSlug() != null) {
                content.append(" (slug: ").append(category.getSlug()).append(")");
            }
            content.append("\n");
        }

        if (brand != null) {
            content.append("Thương hiệu: ").append(brand.getName()).append("\n");
        }

        content.append("Giá niêm yết: ").append(product.getPrice().toPlainString()).append(" VND\n");

        Integer discountPercent = discountsMap.get(product.getId());
        BigDecimal discountPrice = null;
        if (discountPercent != null && discountPercent > 0) {
            discountPrice = productService.calculateDiscountPrice(product.getPrice(), discountPercent);
            content.append("Giá khuyến mãi: ").append(discountPrice.toPlainString())
                   .append(" VND (Giảm ").append(discountPercent).append("%)\n");
        }

        content.append("Số lượng tồn kho: ").append(product.getStock()).append(" sản phẩm\n");

        if (product.getDescription() != null && !product.getDescription().isBlank()) {
            String desc = product.getDescription();
            // Giới hạn độ dài mô tả để không vượt quá context window
            if (desc.length() > 600) desc = desc.substring(0, 600) + "...";
            content.append("Mô tả: ").append(desc).append("\n");
        }

        if (product.getSpecsJson() != null && !product.getSpecsJson().isEmpty()) {
            content.append("Thông số kỹ thuật: ").append(product.getSpecsJson().toString()).append("\n");
        }

        // Metadata cấu trúc dùng để tái tạo RecommendedProductDto
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("productId", product.getId());
        metadata.put("name", product.getName());
        metadata.put("slug", product.getSlug() != null ? product.getSlug() : "");
        metadata.put("price", product.getPrice().toPlainString());
        metadata.put("discountPercent", discountPercent != null ? discountPercent : 0);
        if (discountPrice != null) {
            metadata.put("discountPrice", discountPrice.toPlainString());
        }
        metadata.put("thumbnailUrl", product.getThumbnailUrl() != null ? product.getThumbnailUrl() : "");
        metadata.put("stock", product.getStock());
        metadata.put("categorySlug", category != null && category.getSlug() != null ? category.getSlug() : "");
        metadata.put("source", "product"); // Tag để phân biệt nguồn dữ liệu

        return Document.builder()
                .id("product-" + product.getId())
                .text(content.toString())
                .metadata(metadata)
                .build();
    }

    /**
     * Lấy số lượng sản phẩm đang còn hàng (đã hoặc chưa được index).
     */
    public long getIndexableProductCount() {
        return productRepository.findAll().stream()
                .filter(p -> p.getStock() > 0)
                .count();
    }
}

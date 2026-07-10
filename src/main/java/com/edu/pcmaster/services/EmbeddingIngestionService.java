package com.edu.pcmaster.services;

import com.edu.pcmaster.models.Brand;
import com.edu.pcmaster.models.Category;
import com.edu.pcmaster.models.Product;
import com.edu.pcmaster.repositories.ProductRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class EmbeddingIngestionService {

    private final VectorStore vectorStore;
    private final ProductRepository productRepository;
    private final ProductService productService;
    private final ObjectMapper objectMapper;

    private static final int BATCH_SIZE = 50;

    private static final DecimalFormat PRICE_FORMAT = new DecimalFormat("#,###");

    private static final Map<String, String> CATEGORY_ALIASES = Map.ofEntries(
            Map.entry("cpu", "CPU, vi xử lý, bộ xử lý, processor"),
            Map.entry("vi-xu-ly", "CPU, vi xử lý, bộ xử lý, processor"),
            Map.entry("vga", "VGA, card đồ họa, card màn hình, GPU, graphics card"),
            Map.entry("gpu", "VGA, card đồ họa, card màn hình, GPU, graphics card"),
            Map.entry("card-do-hoa", "VGA, card đồ họa, card màn hình, GPU, graphics card"),
            Map.entry("ram", "RAM, bộ nhớ, memory"),
            Map.entry("bo-nho", "RAM, bộ nhớ, memory"),
            Map.entry("mainboard", "Mainboard, bo mạch chủ, motherboard"),
            Map.entry("bo-mach-chu", "Mainboard, bo mạch chủ, motherboard"),
            Map.entry("ssd", "SSD, ổ cứng thể rắn, solid state drive"),
            Map.entry("hdd", "HDD, ổ cứng, hard drive"),
            Map.entry("o-cung", "ổ cứng, SSD, HDD, storage, lưu trữ"),
            Map.entry("psu", "PSU, nguồn máy tính, nguồn, power supply"),
            Map.entry("nguon", "PSU, nguồn máy tính, nguồn, power supply"),
            Map.entry("nguon-may-tinh", "PSU, nguồn máy tính, nguồn, power supply"),
            Map.entry("case", "Case, vỏ máy tính, vỏ case, thùng máy"),
            Map.entry("vo-may-tinh", "Case, vỏ máy tính, vỏ case, thùng máy"),
            Map.entry("tan-nhiet", "Tản nhiệt, cooler, quạt tản nhiệt, tản nhiệt nước, tản nhiệt khí"),
            Map.entry("cooler", "Tản nhiệt, cooler, quạt tản nhiệt, tản nhiệt nước, tản nhiệt khí"),
            Map.entry("laptop", "Laptop, máy tính xách tay"),
            Map.entry("monitor", "Màn hình, monitor, màn hình máy tính"),
            Map.entry("man-hinh", "Màn hình, monitor, màn hình máy tính"));

    private static final Map<String, List<String>> KEY_SPECS_BY_TYPE = Map.of(
            "CPU", List.of("cores", "threads", "base_clock", "boost_clock", "base_clock_ghz", "boost_clock_ghz",
                    "socket", "tdp", "tdp_w", "lithography", "l3_cache", "memory_support", "ram_type",
                    "p_cores", "e_cores", "generation", "series", "architecture", "integrated_gpu"),
            "GPU", List.of("vram", "base_clock", "boost_clock", "memory_bus", "memory_type",
                    "cuda_cores", "tdp", "recommended_psu", "interface", "ray_tracing", "dlss",
                    "vga_series", "directx", "max_resolution"),
            "RAM", List.of("capacity_gb", "bus_speed_mhz", "ram_type", "latency_cl", "kit", "has_rgb"),
            "MAINBOARD", List.of("chipset", "socket", "form_factor", "ram_type", "ram_slots",
                    "max_ram_gb", "m2_slots", "has_wifi", "pcie_support"),
            "PSU", List.of("wattage", "efficiency_rating", "modularity", "form_factor"),
            "CASE", List.of("case_size", "form_factor", "supported_mainboards", "max_gpu_length_mm",
                    "max_cpu_cooler_height_mm", "fan_count_included", "tempered_glass_side", "color"),
            "STORAGE", List.of("capacity_gb", "interface", "form_factor"),
            "COOLER", List.of("cooler_type", "fan_count", "cpu_socket_support", "fan_size_mm",
                    "fan_speed_rpm", "radiator_dimensions", "has_rgb"));

    private static final Map<String, String> SPEC_DISPLAY_NAMES = Map.ofEntries(
            Map.entry("cores", "Số nhân"),
            Map.entry("threads", "Số luồng"),
            Map.entry("base_clock", "Xung nhịp cơ bản"),
            Map.entry("boost_clock", "Xung nhịp tối đa"),
            Map.entry("base_clock_ghz", "Xung nhịp cơ bản"),
            Map.entry("boost_clock_ghz", "Xung nhịp tối đa"),
            Map.entry("socket", "Socket"),
            Map.entry("tdp", "TDP"),
            Map.entry("tdp_w", "TDP"),
            Map.entry("lithography", "Tiến trình"),
            Map.entry("l3_cache", "Cache L3"),
            Map.entry("memory_support", "Hỗ trợ RAM"),
            Map.entry("ram_type", "Loại RAM"),
            Map.entry("p_cores", "Nhân P-Core"),
            Map.entry("e_cores", "Nhân E-Core"),
            Map.entry("generation", "Thế hệ"),
            Map.entry("series", "Dòng sản phẩm"),
            Map.entry("architecture", "Kiến trúc"),
            Map.entry("integrated_gpu", "Đồ họa tích hợp"),
            Map.entry("vram", "VRAM"),
            Map.entry("memory_bus", "Bus bộ nhớ"),
            Map.entry("memory_type", "Loại bộ nhớ"),
            Map.entry("cuda_cores", "CUDA Cores"),
            Map.entry("recommended_psu", "Nguồn đề xuất"),
            Map.entry("interface", "Chuẩn giao tiếp"),
            Map.entry("ray_tracing", "Ray Tracing"),
            Map.entry("dlss", "DLSS"),
            Map.entry("vga_series", "Dòng VGA"),
            Map.entry("directx", "DirectX"),
            Map.entry("max_resolution", "Độ phân giải tối đa"),
            Map.entry("capacity_gb", "Dung lượng"),
            Map.entry("bus_speed_mhz", "Bus RAM"),
            Map.entry("latency_cl", "CAS Latency"),
            Map.entry("kit", "Số thanh"),
            Map.entry("has_rgb", "LED RGB"),
            Map.entry("chipset", "Chipset"),
            Map.entry("form_factor", "Chuẩn/Kích thước"),
            Map.entry("ram_slots", "Khe RAM"),
            Map.entry("max_ram_gb", "RAM tối đa"),
            Map.entry("m2_slots", "Khe M.2"),
            Map.entry("has_wifi", "Wi-Fi"),
            Map.entry("pcie_support", "PCIe"),
            Map.entry("wattage", "Công suất"),
            Map.entry("efficiency_rating", "Chứng nhận 80+"),
            Map.entry("modularity", "Modular"),
            Map.entry("case_size", "Kích thước"),
            Map.entry("supported_mainboards", "Main hỗ trợ"),
            Map.entry("max_gpu_length_mm", "VGA tối đa"),
            Map.entry("max_cpu_cooler_height_mm", "Tản nhiệt tối đa"),
            Map.entry("fan_count_included", "Quạt đi kèm"),
            Map.entry("tempered_glass_side", "Kính cường lực"),
            Map.entry("color", "Màu sắc"),
            Map.entry("cooler_type", "Loại tản nhiệt"),
            Map.entry("fan_count", "Số quạt"),
            Map.entry("cpu_socket_support", "Socket hỗ trợ"),
            Map.entry("fan_size_mm", "Kích thước quạt"),
            Map.entry("fan_speed_rpm", "Tốc độ quạt"),
            Map.entry("radiator_dimensions", "Kích thước radiator"));

    public EmbeddingIngestionService(VectorStore vectorStore,
            ProductRepository productRepository,
            ProductService productService,
            ObjectMapper objectMapper) {
        this.vectorStore = vectorStore;
        this.productRepository = productRepository;
        this.productService = productService;
        this.objectMapper = objectMapper;
    }

    @org.springframework.transaction.annotation.Transactional
    public int reindexAll() {
        List<Product> allProducts = productRepository.findAll();

        List<String> existingDocIds = allProducts.stream()
                .map(p -> toDocumentId(p.getId()))
                .collect(Collectors.toList());

        if (!existingDocIds.isEmpty()) {
            for (int i = 0; i < existingDocIds.size(); i += BATCH_SIZE) {
                List<String> batch = existingDocIds.subList(i, Math.min(i + BATCH_SIZE, existingDocIds.size()));
                try {
                    vectorStore.delete(batch);
                } catch (Exception e) {

                    for (String docId : batch) {
                        try {
                            vectorStore.delete(List.of(docId));
                        } catch (Exception ignored) {
                        }
                    }
                }
            }
            System.out.printf("[RAG] Deleted existing vectors for %d products.%n", existingDocIds.size());
        }

        Map<Long, Integer> discountsMap = productService.getActiveProductDiscountsMap();

        List<Document> documents = allProducts.stream()
                .filter(p -> p.getStock() > 0)
                .map(p -> buildDocument(p, discountsMap))
                .collect(Collectors.toList());

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

    @org.springframework.transaction.annotation.Transactional
    public void indexProduct(Product product) {
        if (product == null || product.getId() == null)
            return;
        if (product.getStock() <= 0) {
            deleteProduct(product);
            return;
        }

        try {
            Map<Long, Integer> discountsMap = productService.getActiveProductDiscountsMap();
            Document doc = buildDocument(product, discountsMap);
            vectorStore.add(List.of(doc));
            System.out.printf("[RAG] Auto-indexed product ID: %d (%s)%n", product.getId(), product.getName());
        } catch (Exception e) {
            System.err.printf("[RAG] Failed to auto-index product ID %d: %s%n", product.getId(), e.getMessage());
        }
    }

    public void deleteProduct(Product product) {
        if (product == null || product.getId() == null)
            return;
        try {
            String docId = toDocumentId(product.getId());
            vectorStore.delete(List.of(docId));
            System.out.printf("[RAG] Deleted index for product ID: %d%n", product.getId());
        } catch (Exception e) {
            System.err.printf("[RAG] Failed to delete index for product ID %d: %s%n", product.getId(), e.getMessage());
        }
    }

    public long getIndexableProductCount() {
        return productRepository.findAll().stream()
                .filter(p -> p.getStock() > 0)
                .count();
    }

    private Document buildDocument(Product product, Map<Long, Integer> discountsMap) {
        Category category = product.getCategory();
        Brand brand = product.getBrand();
        String catSlug = category != null && category.getSlug() != null ? category.getSlug().toLowerCase() : "";
        String componentType = detectComponentType(product);

        StringBuilder content = new StringBuilder();

        // ── 1. Tên sản phẩm chuẩn hóa (trọng số cao nhất cho embedding)
        if (!componentType.isEmpty()) {
            content.append("[").append(componentType.toUpperCase()).append("] ");
        }
        content.append(product.getName().toUpperCase()).append("\n");

        // Trích xuất mã sản phẩm biến thể để embedding model capture tốt hơn
        String variants = extractModelVariants(product.getName());
        if (!variants.isEmpty()) {
            content.append("Mã sản phẩm: ").append(variants).append("\n");
        }

        if (category != null) {
            content.append("Loại: ").append(category.getName());
            String aliases = findCategoryAliases(catSlug);
            if (aliases != null) {
                content.append(" (").append(aliases).append(")");
            }
            content.append("\n");
        }
        if (!componentType.isEmpty()) {
            content.append("Phân loại linh kiện: ").append(componentType).append("\n");
        }

        if (brand != null) {
            content.append("Thương hiệu: ").append(brand.getName()).append("\n");
        }

        BigDecimal price = product.getPrice();
        if (price != null) {
            Integer discountPercent = discountsMap.get(product.getId());
            BigDecimal effectivePrice = price;
            if (discountPercent != null && discountPercent > 0) {
                effectivePrice = productService.calculateDiscountPrice(price, discountPercent);
                content.append("Giá gốc: ").append(formatPrice(price)).append("\n");
                content.append("Giá khuyến mãi: ").append(formatPrice(effectivePrice))
                        .append(" (giảm ").append(discountPercent).append("%)\n");
            } else {
                content.append("Giá: ").append(formatPrice(price)).append("\n");
            }

            content.append("Phân khúc giá: ").append(priceSegment(effectivePrice)).append("\n");
        }

        if (product.getSpecsJson() != null && !product.getSpecsJson().isNull()) {
            String specsText = buildKeySpecsText(product.getSpecsJson(), componentType);
            if (!specsText.isEmpty()) {
                content.append("Thông số kỹ thuật:\n").append(specsText);
            }
        }

        if (product.getDescription() != null && !product.getDescription().isBlank()) {
            String cleanDesc = stripHtml(product.getDescription());
            if (!cleanDesc.isBlank()) {
                if (cleanDesc.length() > 800)
                    cleanDesc = cleanDesc.substring(0, 800) + "...";
                content.append("Mô tả: ").append(cleanDesc).append("\n");
            }
        }

        Map<String, Object> metadata = buildMetadata(product, category, brand, discountsMap, componentType);

        return Document.builder()
                .id(toDocumentId(product.getId()))
                .text(content.toString())
                .metadata(metadata)
                .build();
    }

    private String buildKeySpecsText(JsonNode specsJson, String componentType) {
        List<String> keySpecs = KEY_SPECS_BY_TYPE.getOrDefault(componentType, List.of());

        StringBuilder sb = new StringBuilder();
        Map<String, String> allSpecs = flattenSpecs(specsJson);

        if (!keySpecs.isEmpty()) {

            for (String key : keySpecs) {
                String value = allSpecs.get(key);
                if (value != null && !value.isBlank()) {
                    String displayName = SPEC_DISPLAY_NAMES.getOrDefault(key, key);
                    sb.append("  - ").append(displayName).append(": ").append(value).append("\n");
                }
            }
        } else {

            int count = 0;
            for (Map.Entry<String, String> entry : allSpecs.entrySet()) {
                if (count >= 15)
                    break;
                String key = entry.getKey();
                if (key.equals("component_type"))
                    continue;
                String value = entry.getValue();
                if (value.length() > 200)
                    continue;
                String displayName = SPEC_DISPLAY_NAMES.getOrDefault(key, key);
                sb.append("  - ").append(displayName).append(": ").append(value).append("\n");
                count++;
            }
        }

        return sb.toString();
    }

    private Map<String, String> flattenSpecs(JsonNode specs) {
        Map<String, String> result = new HashMap<>();
        if (specs == null || !specs.isObject())
            return result;

        Iterator<Map.Entry<String, JsonNode>> fields = specs.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> entry = fields.next();
            String key = entry.getKey();
            JsonNode val = entry.getValue();
            if (val.isTextual()) {
                result.put(key, val.asText());
            } else if (val.isNumber()) {
                result.put(key, val.asText());
            } else if (val.isBoolean()) {
                result.put(key, val.asBoolean() ? "Có" : "Không");
            }
        }
        return result;
    }

    private Map<String, Object> buildMetadata(Product product, Category category, Brand brand,
            Map<Long, Integer> discountsMap, String componentType) {
        Integer discountPercent = discountsMap.get(product.getId());
        BigDecimal discountPrice = null;
        if (discountPercent != null && discountPercent > 0) {
            discountPrice = productService.calculateDiscountPrice(product.getPrice(), discountPercent);
        }

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("productId", product.getId());
        metadata.put("product_id", product.getId().toString());
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
        metadata.put("brandName", brand != null ? brand.getName() : "");
        metadata.put("category", category != null ? category.getName() : "");
        metadata.put("brand", brand != null ? brand.getName() : "");
        metadata.put("componentType", componentType);
        metadata.put("source", "product");

        // ── Ánh xạ Specs JSONB sang Vector Metadata động (Không đổi SQL schema) ──
        JsonNode specs = product.getSpecsJson();
        if (specs != null && specs.isObject()) {
            putSpecIfPresent(metadata, specs, "socket");
            putSpecIfPresent(metadata, specs, "ram_type");
            putSpecIntIfPresent(metadata, specs, "tdp");
            putSpecIfPresent(metadata, specs, "form_factor");
            putSpecIfPresent(metadata, specs, "chipset");
            putSpecIntIfPresent(metadata, specs, "wattage");
            putSpecIntIfPresent(metadata, specs, "gpu_length_mm");
            putSpecIntIfPresent(metadata, specs, "max_gpu_length_mm");
            putSpecIntIfPresent(metadata, specs, "max_cpu_cooler_height_mm");
            putSpecIfPresent(metadata, specs, "cpu_socket_support");
            putSpecIntIfPresent(metadata, specs, "recommended_psu");
            putSpecIntIfPresent(metadata, specs, "cooler_height_mm");
        }

        return metadata;
    }

    private void putSpecIfPresent(Map<String, Object> metadata, JsonNode specs, String key) {
        if (specs.has(key) && !specs.get(key).isNull()) {
            String val = specs.get(key).asText("").trim();
            if (!val.isEmpty()) {
                metadata.put(key, val);
            }
        }
    }

    private void putSpecIntIfPresent(Map<String, Object> metadata, JsonNode specs, String key) {
        if (specs.has(key) && specs.get(key).isNumber()) {
            metadata.put(key, specs.get(key).asInt());
        }
    }

    private String toDocumentId(Long productId) {
        return UUID.nameUUIDFromBytes(("product-" + productId).getBytes()).toString();
    }

    private String detectComponentType(Product product) {
        if (product.getSpecsJson() != null && product.getSpecsJson().has("component_type")) {
            return product.getSpecsJson().get("component_type").asText("");
        }
        Category cat = product.getCategory();
        if (cat == null || cat.getSlug() == null)
            return "";
        String slug = cat.getSlug().toLowerCase().replace("-", "");
        if (slug.contains("psu") || slug.contains("power") || slug.contains("nguon"))
            return "PSU";
        if (slug.contains("ram") || slug.contains("memory") || slug.contains("bonho"))
            return "RAM";
        if (slug.contains("mainboard") || slug.contains("mother") || slug.contains("board")
                || slug.contains("bomachchu"))
            return "MAINBOARD";
        if (slug.contains("vga") || slug.contains("gpu") || slug.contains("graphic") || slug.contains("carddohoa"))
            return "GPU";
        if (slug.contains("cpu") || slug.contains("processor") || slug.contains("vixuly"))
            return "CPU";
        if (slug.contains("ssd") || slug.contains("hdd") || slug.contains("storage") || slug.contains("ocung"))
            return "STORAGE";
        if (slug.contains("case") || slug.contains("vomay"))
            return "CASE";
        if (slug.contains("tannhiet") || slug.contains("cooler") || slug.contains("fan"))
            return "COOLER";
        return "";
    }

    /**
     * Tìm category aliases (từ đồng nghĩa) cho một category slug.
     */
    private String findCategoryAliases(String catSlug) {
        if (catSlug == null || catSlug.isEmpty())
            return null;
        // Direct match
        String aliases = CATEGORY_ALIASES.get(catSlug);
        if (aliases != null)
            return aliases;
        // Partial match
        for (Map.Entry<String, String> entry : CATEGORY_ALIASES.entrySet()) {
            if (catSlug.contains(entry.getKey()))
                return entry.getValue();
        }
        return null;
    }

    /**
     * Strip HTML tags khỏi description, giữ text thuần.
     */
    private String stripHtml(String html) {
        if (html == null)
            return "";
        // Remove HTML tags
        String text = html.replaceAll("<[^>]+>", " ");
        // Remove HTML entities
        text = text.replaceAll("&nbsp;", " ")
                .replaceAll("&amp;", "&")
                .replaceAll("&lt;", "<")
                .replaceAll("&gt;", ">")
                .replaceAll("&quot;", "\"")
                .replaceAll("&#\\d+;", " ");
        // Collapse whitespace
        text = text.replaceAll("\\s+", " ").trim();
        return text;
    }

    /**
     * Format giá thành chuỗi dễ đọc: "12.990.000 VND"
     */
    private String formatPrice(BigDecimal price) {
        return PRICE_FORMAT.format(price) + " VND";
    }

    private String priceSegment(BigDecimal price) {
        long million = price.longValue() / 1_000_000;
        if (million < 1)
            return "dưới 1 triệu";
        if (million < 3)
            return "khoảng " + million + " triệu, phân khúc phổ thông";
        if (million < 5)
            return "khoảng " + million + " triệu, phân khúc tầm trung thấp";
        if (million < 10)
            return "khoảng " + million + " triệu, phân khúc tầm trung";
        if (million < 20)
            return "khoảng " + million + " triệu, phân khúc tầm trung cao";
        if (million < 35)
            return "khoảng " + million + " triệu, phân khúc cao cấp";
        return "khoảng " + million + " triệu, phân khúc siêu cao cấp";
    }

    private String extractModelVariants(String productName) {
        List<String> variants = new ArrayList<>();
        // GPU
        Matcher gpuMatcher = Pattern.compile("(?i)((?:RTX|GTX|RX|ARC)\\s*\\d{3,4}(?:\\s*(?:TI|XT|XTX|SUPER))*)")
                .matcher(productName);
        while (gpuMatcher.find()) {
            variants.add(gpuMatcher.group(1).toUpperCase().trim());
        }
        // CPU
        Matcher cpuMatcher = Pattern.compile("(?i)((?:RYZEN\\s*[3579]\\s*\\d{4}\\w*)|(?:CORE\\s*I[3579][-\\s]*\\d{4,5}\\w*))")
                .matcher(productName);
        while (cpuMatcher.find()) {
            variants.add(cpuMatcher.group(1).toUpperCase().trim());
        }
        // Chipset
        Matcher chipMatcher = Pattern.compile("(?i)\\b([BXZHAW]\\d{3}[MESP]*)\\b")
                .matcher(productName);
        while (chipMatcher.find()) {
            variants.add(chipMatcher.group(1).toUpperCase().trim());
        }
        return String.join(", ", variants);
    }
}

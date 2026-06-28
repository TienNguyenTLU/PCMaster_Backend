package com.edu.pcmaster.services;

import com.edu.pcmaster.models.Product;
import com.edu.pcmaster.repositories.ProductRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class ProductDetailsFunction implements Function<ProductDetailsFunction.Request, ProductDetailsFunction.Response> {

    private final ProductRepository productRepository;
    private final ProductService productService;
    private final ObjectMapper objectMapper;

    public ProductDetailsFunction(ProductRepository productRepository, 
                                  ProductService productService,
                                  ObjectMapper objectMapper) {
        this.productRepository = productRepository;
        this.productService = productService;
        this.objectMapper = objectMapper;
    }

    public record Request(Long productId) {}

    public record Response(
            Long productId,
            String name,
            String price,
            String discountPrice,
            Integer discountPercent,
            Integer stock,
            Map<String, Object> specs
    ) {}

    @Override
    public Response apply(Request request) {
        if (request.productId() == null) {
            return new Response(null, "Missing product ID", "0", "0", 0, 0, Map.of());
        }

        var productOpt = productRepository.findById(request.productId());
        if (productOpt.isEmpty()) {
            return new Response(request.productId(), "Sản phẩm không tồn tại", "0", "0", 0, 0, Map.of());
        }

        Product product = productOpt.get();

        // Calculate dynamic active discount
        Map<Long, Integer> discountsMap = productService.getActiveProductDiscountsMap();
        Integer discountPercent = discountsMap.getOrDefault(product.getId(), 0);
        BigDecimal discountPrice = null;
        if (discountPercent > 0) {
            discountPrice = productService.calculateDiscountPrice(product.getPrice(), discountPercent);
        }

        // Parse specs JSON
        Map<String, Object> specsMap = new HashMap<>();
        if (product.getSpecsJson() != null && !product.getSpecsJson().isNull()) {
            try {
                specsMap = objectMapper.convertValue(product.getSpecsJson(), Map.class);
            } catch (Exception ignored) {
                // If conversions fail, put raw string representation
                specsMap.put("specs_raw", product.getSpecsJson().toString());
            }
        }

        return new Response(
                product.getId(),
                product.getName(),
                product.getPrice().toPlainString() + " VND",
                discountPrice != null ? discountPrice.toPlainString() + " VND" : product.getPrice().toPlainString() + " VND",
                discountPercent,
                product.getStock(),
                specsMap
        );
    }
}

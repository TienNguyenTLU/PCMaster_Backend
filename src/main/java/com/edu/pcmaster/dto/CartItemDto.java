package com.edu.pcmaster.dto;

import java.math.BigDecimal;
import lombok.Data;

@Data
public class CartItemDto {
    private Long id;
    private Long productId;
    private String productName;
    private String productThumbnailUrl;
    private BigDecimal productPrice;
    private Integer productStock;
    private Integer quantity;
}

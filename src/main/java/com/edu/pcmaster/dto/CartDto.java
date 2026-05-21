package com.edu.pcmaster.dto;

import java.util.List;
import lombok.Data;

@Data
public class CartDto {
    private Long id;
    private List<CartItemDto> items;
}

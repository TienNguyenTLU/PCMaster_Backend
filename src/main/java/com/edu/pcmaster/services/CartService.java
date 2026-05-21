package com.edu.pcmaster.services;

import com.edu.pcmaster.dto.AddToCartRequest;
import com.edu.pcmaster.dto.CartDto;

public interface CartService {
    CartDto getCartForUser(Long userId);
    CartDto addToCart(Long userId, AddToCartRequest request);
    CartDto updateQuantity(Long userId, Long cartItemId, Integer quantity);
    CartDto removeItem(Long userId, Long cartItemId);
    void clearCart(Long userId);
}

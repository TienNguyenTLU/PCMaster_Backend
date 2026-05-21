package com.edu.pcmaster.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.edu.pcmaster.dto.AddToCartRequest;
import com.edu.pcmaster.dto.CartDto;
import com.edu.pcmaster.dto.UpdateCartItemRequest;
import com.edu.pcmaster.security.UserPrincipal;
import com.edu.pcmaster.services.CartService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CartController {
    private final CartService cartService;

    @GetMapping
    public ResponseEntity<CartDto> getCart(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        Long userId = userPrincipal.getUser().getId();
        return ResponseEntity.ok(cartService.getCartForUser(userId));
    }

    @PostMapping("/items")
    public ResponseEntity<CartDto> addToCart(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestBody AddToCartRequest request) {
        Long userId = userPrincipal.getUser().getId();
        return ResponseEntity.ok(cartService.addToCart(userId, request));
    }

    @PutMapping("/items/{itemId}")
    public ResponseEntity<CartDto> updateQuantity(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long itemId,
            @RequestBody UpdateCartItemRequest request) {
        Long userId = userPrincipal.getUser().getId();
        return ResponseEntity.ok(cartService.updateQuantity(userId, itemId, request.getQuantity()));
    }

    @DeleteMapping("/items/{itemId}")
    public ResponseEntity<CartDto> removeItem(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long itemId) {
        Long userId = userPrincipal.getUser().getId();
        return ResponseEntity.ok(cartService.removeItem(userId, itemId));
    }

    @DeleteMapping
    public ResponseEntity<Void> clearCart(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        Long userId = userPrincipal.getUser().getId();
        cartService.clearCart(userId);
        return ResponseEntity.noContent().build();
    }
}

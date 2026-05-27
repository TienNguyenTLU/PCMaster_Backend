package com.edu.pcmaster.services;

import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.edu.pcmaster.common.exception.ResourceNotFoundException;
import com.edu.pcmaster.dto.AddToCartRequest;
import com.edu.pcmaster.dto.CartDto;
import com.edu.pcmaster.dto.CartItemDto;
import com.edu.pcmaster.models.Cart;
import com.edu.pcmaster.models.CartItem;
import com.edu.pcmaster.models.Product;
import com.edu.pcmaster.models.User;
import com.edu.pcmaster.repositories.CartItemRepository;
import com.edu.pcmaster.repositories.CartRepository;
import com.edu.pcmaster.repositories.ProductRepository;
import com.edu.pcmaster.repositories.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final ProductService productService;

    private Cart getOrCreateCart(Long userId) {
        return cartRepository.findByUserId(userId).orElseGet(() -> {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found"));
            Cart cart = new Cart();
            cart.setUser(user);
            return cartRepository.save(cart);
        });
    }

    private CartDto mapToDto(Cart cart) {
        CartDto dto = new CartDto();
        dto.setId(cart.getId());
        java.util.Map<Long, Integer> discountsMap = productService.getActiveProductDiscountsMap();
        dto.setItems(cart.getItems().stream().map(item -> {
            CartItemDto itemDto = new CartItemDto();
            itemDto.setId(item.getId());
            itemDto.setProductId(item.getProduct().getId());
            itemDto.setProductName(item.getProduct().getName());
            itemDto.setProductThumbnailUrl(item.getProduct().getThumbnailUrl());
            itemDto.setProductPrice(item.getProduct().getPrice());
            Integer discountPercent = discountsMap.get(item.getProduct().getId());
            java.math.BigDecimal discountPrice = productService.calculateDiscountPrice(item.getProduct().getPrice(), discountPercent);
            itemDto.setProductDiscountPrice(discountPrice);
            itemDto.setProductStock(item.getProduct().getStock());
            itemDto.setQuantity(item.getQuantity());
            return itemDto;
        }).collect(Collectors.toList()));
        return dto;
    }

    @Override
    @Transactional(readOnly = true)
    public CartDto getCartForUser(Long userId) {
        Cart cart = getOrCreateCart(userId);
        return mapToDto(cart);
    }

    @Override
    @Transactional
    public CartDto addToCart(Long userId, AddToCartRequest request) {
        Cart cart = getOrCreateCart(userId);
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        Optional<CartItem> existingItem = cart.getItems().stream()
                .filter(item -> item.getProduct().getId().equals(product.getId()))
                .findFirst();

        if (existingItem.isPresent()) {
            CartItem item = existingItem.get();
            item.setQuantity(Math.min(product.getStock(), item.getQuantity() + request.getQuantity()));
        } else {
            CartItem item = new CartItem();
            item.setCart(cart);
            item.setProduct(product);
            item.setQuantity(Math.min(product.getStock(), request.getQuantity()));
            cart.getItems().add(item);
        }

        Cart savedCart = cartRepository.save(cart);
        return mapToDto(savedCart);
    }

    @Override
    @Transactional
    public CartDto updateQuantity(Long userId, Long cartItemId, Integer quantity) {
        Cart cart = getOrCreateCart(userId);
        CartItem item = cart.getItems().stream()
                .filter(i -> i.getId().equals(cartItemId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("CartItem not found"));
        
        item.setQuantity(Math.min(item.getProduct().getStock(), Math.max(1, quantity)));
        Cart savedCart = cartRepository.save(cart);
        return mapToDto(savedCart);
    }

    @Override
    @Transactional
    public CartDto removeItem(Long userId, Long cartItemId) {
        Cart cart = getOrCreateCart(userId);
        cart.getItems().removeIf(item -> item.getId().equals(cartItemId));
        Cart savedCart = cartRepository.save(cart);
        return mapToDto(savedCart);
    }

    @Override
    @Transactional
    public void clearCart(Long userId) {
        Cart cart = getOrCreateCart(userId);
        cart.getItems().clear();
        cartRepository.save(cart);
    }
}

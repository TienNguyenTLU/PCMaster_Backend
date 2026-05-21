package com.edu.pcmaster.controllers;

import com.edu.pcmaster.models.ProductImage;
import com.edu.pcmaster.services.ProductImageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import org.springframework.security.access.prepost.PreAuthorize;
import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/product-images")
@RequiredArgsConstructor
public class ProductImageController {

    private final ProductImageService productImageService;

    @GetMapping("/product/{productId}")
    public ResponseEntity<List<ProductImage>> getImagesByProductId(@PathVariable Long productId) {
        return ResponseEntity.ok(productImageService.getImagesByProductId(productId));
    }

    @PostMapping("/product/{productId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProductImage> uploadImage(
            @PathVariable Long productId,
            @RequestParam("file") MultipartFile file) throws IOException {
        ProductImage image = productImageService.uploadImage(productId, file);
        return ResponseEntity.ok(image);
    }

    @DeleteMapping("/{imageId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteImage(@PathVariable Long imageId) {
        productImageService.deleteImage(imageId);
        return ResponseEntity.noContent().build();
    }
}

package com.edu.pcmaster.services;

import com.edu.pcmaster.models.Product;
import com.edu.pcmaster.models.ProductImage;
import com.edu.pcmaster.repositories.ProductImageRepository;
import com.edu.pcmaster.repositories.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ProductImageService {

    private final ProductImageRepository productImageRepository;
    private final ProductRepository productRepository;
    private final MediaService mediaService;
    private static final String CLOUD_ROOT = "PCMAster_Storage";

    public List<ProductImage> getImagesByProductId(Long productId) {
        return productImageRepository.findByProductId(productId);
    }

    public ProductImage uploadImage(Long productId, MultipartFile file) throws IOException {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        String folder = String.format("%s/Product_detail_Image/%s", CLOUD_ROOT, product.getSlug());
        String publicId = file.getOriginalFilename();
        String imageUrl = mediaService.upload(file.getBytes(), folder, publicId);

        ProductImage productImage = new ProductImage();
        productImage.setProduct(product);
        productImage.setUrl(imageUrl);
        return productImageRepository.save(productImage);
    }

    public void deleteImage(Long imageId) {
        ProductImage productImage = productImageRepository.findById(imageId)
                .orElseThrow(() -> new RuntimeException("Image not found"));
        
        // Extract public ID from URL to delete from Cloudinary
        String url = productImage.getUrl();
        try {
            String publicId = extractPublicIdFromUrl(url);
            mediaService.delete(publicId);
        } catch (Exception e) {
            // Log the error but proceed with deleting from the database
            System.err.println("Failed to delete image from Cloudinary: " + e.getMessage());
        }

        productImageRepository.delete(productImage);
    }

    private String extractPublicIdFromUrl(String url) {
        // Example URL: http://res.cloudinary.com/demo/image/upload/v1572285634/folder/public_id.jpg
        // We need to extract "folder/public_id"
        int uploadIndex = url.indexOf("/upload/");
        if (uploadIndex == -1) {
            throw new IllegalArgumentException("Invalid Cloudinary URL");
        }
        
        // Find the version part (e.g., /v1572285634/)
        int versionIndex = url.indexOf("/v", uploadIndex + 8);
        if (versionIndex == -1) {
            throw new IllegalArgumentException("Invalid Cloudinary URL: Missing version");
        }

        int startIndex = url.indexOf('/', versionIndex + 1) + 1;
        int endIndex = url.lastIndexOf('.');
        
        if (startIndex == -1 || endIndex == -1 || startIndex >= endIndex) {
            throw new IllegalArgumentException("Invalid Cloudinary URL: Cannot extract public ID");
        }

        return url.substring(startIndex, endIndex);
    }
}

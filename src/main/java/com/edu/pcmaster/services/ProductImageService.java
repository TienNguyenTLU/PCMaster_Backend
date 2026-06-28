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
        
        
        String url = productImage.getUrl();
        try {
            String publicId = extractPublicIdFromUrl(url);
            mediaService.delete(publicId);
        } catch (Exception e) {
            
            System.err.println("Failed to delete image from Cloudinary: " + e.getMessage());
        }

        productImageRepository.delete(productImage);
    }

    private String extractPublicIdFromUrl(String url) {
        int uploadIndex = url.indexOf("/upload/");
        if (uploadIndex == -1) {
            throw new IllegalArgumentException("Invalid Cloudinary URL");
        }
        
        String path = url.substring(uploadIndex + 8);
        int lastDot = path.lastIndexOf('.');
        if (lastDot != -1) {
            path = path.substring(0, lastDot);
        }
        
        String[] segments = path.split("/");
        StringBuilder publicIdBuilder = new StringBuilder();
        boolean foundPublicIdStart = false;
        
        for (String segment : segments) {
            if (segment.isEmpty()) {
                continue;
            }
            if (!foundPublicIdStart) {
                if (segment.matches("v\\d+")) {
                    foundPublicIdStart = true;
                    continue;
                }
                if (isTransformation(segment)) {
                    continue;
                }
                foundPublicIdStart = true;
            }
            
            if (foundPublicIdStart) {
                if (publicIdBuilder.length() > 0) {
                    publicIdBuilder.append("/");
                }
                publicIdBuilder.append(segment);
            }
        }
        
        if (publicIdBuilder.length() == 0) {
            throw new IllegalArgumentException("Invalid Cloudinary URL: Cannot extract public ID");
        }
        
        return publicIdBuilder.toString();
    }

    private boolean isTransformation(String segment) {
        String regex = "^(?:(c|dpr|e|f|fl|g|h|l|p|q|r|t|u|w|x|y|z|ac|br|co|dl|dn|du|eo|fps|ki|so|vc|vs|b|o|a|d|cs)_[a-zA-Z0-9-._]+)(?:,(?:(c|dpr|e|f|fl|g|h|l|p|q|r|t|u|w|x|y|z|ac|br|co|dl|dn|du|eo|fps|ki|so|vc|vs|b|o|a|d|cs)_[a-zA-Z0-9-._]+))*$";
        return segment.matches(regex);
    }
}

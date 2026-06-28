package com.edu.pcmaster.services;

import com.edu.pcmaster.models.Banner;
import com.edu.pcmaster.repositories.BannerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BannerService {

    private final BannerRepository bannerRepository;
    private final MediaService mediaService;
    private static final String CLOUD_FOLDER = "PCMAster_Storage/Banners";

    public List<Banner> getAllBanners() {
        return bannerRepository.findAllByOrderByDisplayOrderAsc();
    }

    public Banner createBanner(MultipartFile file, String linkUrl, int displayOrder) throws IOException {
        String imageUrl = mediaService.upload(file, CLOUD_FOLDER);
        Banner banner = new Banner();
        banner.setImageUrl(imageUrl);
        banner.setLinkUrl(linkUrl);
        banner.setDisplayOrder(displayOrder);
        return bannerRepository.save(banner);
    }

    public Banner updateBanner(Long id, MultipartFile file, String linkUrl, Integer displayOrder) throws IOException {
        Banner banner = bannerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Banner not found"));

        if (file != null && !file.isEmpty()) {
            String imageUrl = mediaService.upload(file, CLOUD_FOLDER);
            banner.setImageUrl(imageUrl);
        }

        if (linkUrl != null) {
            banner.setLinkUrl(linkUrl);
        }

        if (displayOrder != null) {
            banner.setDisplayOrder(displayOrder);
        }

        return bannerRepository.save(banner);
    }

    public void deleteBanner(Long id) {
        Banner banner = bannerRepository.findById(id).orElse(null);
        if (banner != null) {
            String url = banner.getImageUrl();
            if (url != null && url.contains("cloudinary.com")) {
                try {
                    String publicId = extractPublicIdFromUrl(url);
                    mediaService.delete(publicId);
                } catch (Exception e) {
                    System.err.println("Failed to delete banner image from Cloudinary: " + e.getMessage());
                }
            }
            bannerRepository.delete(banner);
        }
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

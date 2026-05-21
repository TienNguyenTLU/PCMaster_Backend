package com.edu.pcmaster.controllers;

import com.edu.pcmaster.models.Banner;
import com.edu.pcmaster.services.BannerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import org.springframework.security.access.prepost.PreAuthorize;
import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/banners")
@RequiredArgsConstructor
public class BannerController {

    private final BannerService bannerService;

    @GetMapping
    public ResponseEntity<List<Banner>> getAllBanners() {
        return ResponseEntity.ok(bannerService.getAllBanners());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Banner> createBanner(
            @RequestParam("file") MultipartFile file,
            @RequestParam("linkUrl") String linkUrl,
            @RequestParam("displayOrder") int displayOrder) throws IOException {
        Banner banner = bannerService.createBanner(file, linkUrl, displayOrder);
        return ResponseEntity.ok(banner);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Banner> updateBanner(
            @PathVariable Long id,
            @RequestParam(value = "file", required = false) MultipartFile file,
            @RequestParam(value = "linkUrl", required = false) String linkUrl,
            @RequestParam(value = "displayOrder", required = false) Integer displayOrder) throws IOException {
        Banner banner = bannerService.updateBanner(id, file, linkUrl, displayOrder);
        return ResponseEntity.ok(banner);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteBanner(@PathVariable Long id) {
        bannerService.deleteBanner(id);
        return ResponseEntity.noContent().build();
    }
}

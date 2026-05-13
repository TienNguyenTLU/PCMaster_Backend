package com.edu.pcmaster.controllers;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.edu.pcmaster.dto.media.MediaUploadResponse;
import com.edu.pcmaster.services.MediaService;

@RestController
@RequestMapping("/api/admin/media")
@PreAuthorize("hasRole('ADMIN')")
public class MediaController {
	private final MediaService mediaService;

	public MediaController(MediaService mediaService) {
		this.mediaService = mediaService;
	}

	@PostMapping("/upload")
	public MediaUploadResponse upload(@RequestParam("file") MultipartFile file) {
		String url = mediaService.upload(file);
		return new MediaUploadResponse(url);
	}
}


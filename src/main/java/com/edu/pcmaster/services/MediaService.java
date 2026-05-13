package com.edu.pcmaster.services;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.edu.pcmaster.common.exception.BadRequestException;

@Service
public class MediaService {
	private final Cloudinary cloudinary;

	public MediaService(Cloudinary cloudinary) {
		this.cloudinary = cloudinary;
	}

	public String upload(MultipartFile file) {
		return upload(file, "pcmaster");
	}

	public String upload(MultipartFile file, String folder) {
		try {
			Map<?, ?> result = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.asMap(
					"folder", folder
			));
			return result.get("secure_url").toString();
		} catch (IOException ex) {
			throw new BadRequestException("Upload failed");
		}
	}

	public String upload(File file, String folder, String publicId) {
		try {
			Map<?, ?> result = cloudinary.uploader().upload(file, ObjectUtils.asMap(
					"folder", folder,
					"public_id", publicId,
					"overwrite", true
			));
			return result.get("secure_url").toString();
		} catch (IOException ex) {
			throw new BadRequestException("Upload failed");
		}
	}
}

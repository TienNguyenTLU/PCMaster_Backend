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
		validateFileSize(file);
		return upload(file, "pcmaster");
	}

	public String upload(MultipartFile file, String folder) {
		validateFileSize(file);
		try {
			Map<?, ?> result = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.asMap(
					"folder", folder
			));
			return result.get("secure_url").toString();
		} catch (IOException ex) {
			throw new BadRequestException("Upload failed");
		}
	}

	public String upload(byte[] fileBytes, String folder, String publicId) {
		validateFileSize(fileBytes);
		try {
			Map<?, ?> result = cloudinary.uploader().upload(fileBytes, ObjectUtils.asMap(
					"folder", folder,
					"public_id", publicId,
					"overwrite", true
			));
			return result.get("secure_url").toString();
		} catch (IOException ex) {
			throw new BadRequestException("Upload failed");
		}
	}

	public String upload(File file, String folder, String publicId) {
		validateFileSize(file);
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

	public String uploadRaw(MultipartFile file, String folder) {
		validateFileSize(file);
		try {
			Map<?, ?> result = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.asMap(
					"folder", folder,
					"resource_type", "raw",
					"use_filename", true,
					"unique_filename", true
			));
			return result.get("secure_url").toString();
		} catch (IOException ex) {
			throw new BadRequestException("Upload failed");
		}
	}

	public String uploadRaw(byte[] bytes, String folder, String publicId) {
		validateFileSize(bytes);
		try {
			Map<?, ?> result = cloudinary.uploader().upload(bytes, ObjectUtils.asMap(
					"folder", folder,
					"public_id", publicId,
					"resource_type", "raw",
					"overwrite", true
			));
			return result.get("secure_url").toString();
		} catch (IOException ex) {
			throw new BadRequestException("Upload failed");
		}
	}

    public void delete(String publicId) {
        try {
            cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
        } catch (IOException ex) {
            throw new BadRequestException("Delete failed");
        }
    }

	private void validateFileSize(MultipartFile file) {
		if (file == null || file.isEmpty()) {
			throw new BadRequestException("Tệp tin tải lên không hợp lệ hoặc trống");
		}
		if (file.getSize() > 20 * 1024 * 1024) { // 20 MB
			throw new BadRequestException("Dung lượng tệp tin vượt quá giới hạn tối đa cho phép là 20MB");
		}
	}

	private void validateFileSize(byte[] fileBytes) {
		if (fileBytes == null || fileBytes.length == 0) {
			throw new BadRequestException("Dữ liệu tệp tin không hợp lệ hoặc trống");
		}
		if (fileBytes.length > 20 * 1024 * 1024) { // 20 MB
			throw new BadRequestException("Dung lượng tệp tin vượt quá giới hạn tối đa cho phép là 20MB");
		}
	}

	private void validateFileSize(File file) {
		if (file == null || !file.exists()) {
			throw new BadRequestException("Tệp tin không tồn tại");
		}
		if (file.length() > 20 * 1024 * 1024) { // 20 MB
			throw new BadRequestException("Dung lượng tệp tin vượt quá giới hạn tối đa cho phép là 20MB");
		}
	}
}

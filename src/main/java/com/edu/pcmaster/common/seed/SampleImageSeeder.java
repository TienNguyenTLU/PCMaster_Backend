package com.edu.pcmaster.common.seed;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.edu.pcmaster.models.Brand;
import com.edu.pcmaster.models.Category;
import com.edu.pcmaster.repositories.BrandRepository;
import com.edu.pcmaster.repositories.CategoryRepository;
import com.edu.pcmaster.services.MediaService;

@Component
@ConditionalOnProperty(prefix = "app.seed.sample-images", name = "enabled", havingValue = "true")
public class SampleImageSeeder implements CommandLineRunner {
	private static final Logger log = LoggerFactory.getLogger(SampleImageSeeder.class);
	private static final String CLOUD_ROOT = "PCMAster_Storage";
	private static final Set<String> IMAGE_EXTENSIONS = Set.of(".png", ".jpg", ".jpeg", ".webp");

	private final BrandRepository brandRepository;
	private final CategoryRepository categoryRepository;
	private final MediaService mediaService;
	private final String seedRoot;

	public SampleImageSeeder(BrandRepository brandRepository,
							 CategoryRepository categoryRepository,
							 MediaService mediaService,
							 @Value("${app.seed.sample-images.root:Sample_image}") String seedRoot) {
		this.brandRepository = brandRepository;
		this.categoryRepository = categoryRepository;
		this.mediaService = mediaService;
		this.seedRoot = seedRoot;
	}

	@Override
	@Transactional
	public void run(String... args) {
		Path root = Paths.get(seedRoot).toAbsolutePath().normalize();
		Path logosRoot = root.resolve("Brands_Logo");
		Path productsRoot = root.resolve("Product_Thumbnails");
		if (!Files.exists(root)) {
			log.warn("Seed root not found: {}", root);
			return;
		}

		Map<String, Category> categories = seedCategories(logosRoot, productsRoot);
		Map<String, Brand> brandByName = brandRepository.findAll().stream()
				.collect(Collectors.toMap(
						brand -> normalizeKey(brand.getName()),
						brand -> brand,
						(existing, ignored) -> existing
				));
		seedBrandLogos(logosRoot, categories, brandByName);
	}

	private Map<String, Category> seedCategories(Path logosRoot, Path productsRoot) {
		List<Path> categoryDirs = new ArrayList<>();
		categoryDirs.addAll(listDirectories(logosRoot));
		categoryDirs.addAll(listDirectories(productsRoot));

		Map<String, Category> categories = new HashMap<>();
		for (Path dir : categoryDirs) {
			String name = dir.getFileName().toString();
			String slug = slugify(name);
			Category category = categoryRepository.findBySlug(slug)
					.orElseGet(() -> createCategory(name, slug));
			categories.put(name.toLowerCase(Locale.ROOT), category);
		}
		return categories;
	}

	private Category createCategory(String name, String slug) {
		Category category = new Category();
		category.setName(name);
		category.setSlug(slug);
		return categoryRepository.save(category);
	}

	private Map<String, List<Brand>> seedBrandLogos(Path logosRoot, Map<String, Category> categories,
											  Map<String, Brand> brandByName) {
		Map<String, List<Brand>> brandsByCategory = new HashMap<>();
		List<Brand> toSave = new ArrayList<>();
		for (Path categoryDir : listDirectories(logosRoot)) {
			String categoryName = categoryDir.getFileName().toString();
			Category category = categories.get(categoryName.toLowerCase(Locale.ROOT));
			if (category == null) {
				continue;
			}

			List<Brand> brands = new ArrayList<>();
			for (Path logoPath : listImageFiles(categoryDir)) {
				String rawName = stripExtension(logoPath.getFileName().toString());
				String displayName = toDisplayName(rawName);
				String key = normalizeKey(displayName);
				Brand brand = brandByName.get(key);
				if (brand == null) {
					brand = new Brand();
					brand.setName(displayName);
					brandByName.put(key, brand);
				}

				String publicId = slugify(rawName);
				String folder = String.format("%s/Brands_Logos/%s", CLOUD_ROOT, categoryName);
				String logoUrl = mediaService.upload(logoPath.toFile(), folder, publicId);
				if (brand.getLogoUrl() == null || brand.getLogoUrl().isBlank()) {
					brand.setLogoUrl(logoUrl);
					toSave.add(brand);
				}
				brands.add(brand);
			}
			brandsByCategory.put(categoryName.toLowerCase(Locale.ROOT), brands);
		}

		if (!toSave.isEmpty()) {
			brandRepository.saveAll(toSave);
		}
		return brandsByCategory;
	}

	private List<Path> listDirectories(Path root) {
		if (!Files.exists(root)) {
			return List.of();
		}
		try (Stream<Path> stream = Files.list(root)) {
			return stream
					.filter(Files::isDirectory)
					.collect(Collectors.toList());
		} catch (IOException ex) {
			log.warn("Failed to list directories: {}", root, ex);
			return List.of();
		}
	}

	private List<Path> listImageFiles(Path root) {
		if (!Files.exists(root)) {
			return List.of();
		}
		try (Stream<Path> stream = Files.list(root)) {
			return stream
					.filter(Files::isRegularFile)
					.filter(this::isImageFile)
					.collect(Collectors.toList());
		} catch (IOException ex) {
			log.warn("Failed to list files: {}", root, ex);
			return List.of();
		}
	}

	private boolean isImageFile(Path path) {
		String name = path.getFileName().toString().toLowerCase(Locale.ROOT);
		for (String ext : IMAGE_EXTENSIONS) {
			if (name.endsWith(ext)) {
				return true;
			}
		}
		return false;
	}

	private String stripExtension(String filename) {
		int index = filename.lastIndexOf('.');
		return index > 0 ? filename.substring(0, index) : filename;
	}

	private String toDisplayName(String rawName) {
		String cleaned = rawName.replace('_', ' ').replace('-', ' ').trim();
		if (cleaned.isBlank()) {
			return rawName;
		}
		String[] parts = cleaned.split("\\s+");
		StringBuilder builder = new StringBuilder();
		for (String part : parts) {
			if (part.isBlank()) {
				continue;
			}
			builder.append(Character.toUpperCase(part.charAt(0)))
					.append(part.substring(1).toLowerCase(Locale.ROOT))
					.append(' ');
		}
		return builder.toString().trim();
	}

	private String slugify(String input) {
		String slug = input.toLowerCase(Locale.ROOT)
				.replaceAll("[^a-z0-9]+", "-")
				.replaceAll("(^-|-$)", "");
		return slug.isBlank() ? "item" : slug;
	}

	private String normalizeKey(String input) {
		return input == null ? "" : input.trim().toLowerCase(Locale.ROOT);
	}
}

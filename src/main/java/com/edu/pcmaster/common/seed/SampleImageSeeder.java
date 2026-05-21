package com.edu.pcmaster.common.seed;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.edu.pcmaster.models.Brand;
import com.edu.pcmaster.models.Category;
import com.edu.pcmaster.models.Product;
import com.edu.pcmaster.repositories.BrandRepository;
import com.edu.pcmaster.repositories.CategoryRepository;
import com.edu.pcmaster.repositories.ProductRepository;
import com.edu.pcmaster.services.MediaService;

@Component
@ConditionalOnProperty(prefix = "app.seed.sample-images", name = "enabled", havingValue = "true")
public class SampleImageSeeder implements CommandLineRunner {
	private static final Logger log = LoggerFactory.getLogger(SampleImageSeeder.class);
	private static final String CLOUD_ROOT = "PCMAster_Storage";
	private static final Set<String> IMAGE_EXTENSIONS = Set.of(".png", ".jpg", ".jpeg", ".webp");

	private final BrandRepository brandRepository;
	private final CategoryRepository categoryRepository;
	private final ProductRepository productRepository;
	private final MediaService mediaService;
	private final ObjectMapper objectMapper;
	private final String seedRoot;

	public SampleImageSeeder(BrandRepository brandRepository,
							 CategoryRepository categoryRepository,
							 ProductRepository productRepository,
							 MediaService mediaService,
							 ObjectMapper objectMapper,
							 @Value("${app.seed.sample-images.root:Sample_image}") String seedRoot) {
		this.brandRepository = brandRepository;
		this.categoryRepository = categoryRepository;
		this.productRepository = productRepository;
		this.mediaService = mediaService;
		this.objectMapper = objectMapper;
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
		Map<String, List<Brand>> brandsByCategory = seedBrandLogos(logosRoot, categories, brandByName);
		seedProductThumbnails(productsRoot, categories, brandsByCategory);
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

	private void seedProductThumbnails(Path productsRoot,
								  Map<String, Category> categories,
								  Map<String, List<Brand>> brandsByCategory) {
		for (Path categoryDir : listDirectories(productsRoot)) {
			String categoryName = categoryDir.getFileName().toString();
			Category category = categories.get(categoryName.toLowerCase(Locale.ROOT));
			if (category == null) {
				continue;
			}

			List<Brand> brands = brandsByCategory.getOrDefault(categoryName.toLowerCase(Locale.ROOT), List.of());
			for (Path imagePath : listImageFiles(categoryDir)) {
				String rawName = stripExtension(imagePath.getFileName().toString());
				String displayName = toDisplayName(rawName);
				String slug = slugify(rawName);

				// Skip products that are just brand logos inside the product folder
				if (rawName.equalsIgnoreCase("amd") || rawName.equalsIgnoreCase("intel") || rawName.equalsIgnoreCase("asus") ||
					rawName.equalsIgnoreCase("palit") || rawName.equalsIgnoreCase("asrock") || rawName.equalsIgnoreCase("inno3d") ||
					rawName.equalsIgnoreCase("colorful") || rawName.equalsIgnoreCase("gigabyte") || rawName.equalsIgnoreCase("hp") ||
					rawName.equalsIgnoreCase("acer") || rawName.equalsIgnoreCase("gskill") || rawName.equalsIgnoreCase("corsair") ||
					rawName.equalsIgnoreCase("samsung") || rawName.equalsIgnoreCase("kingston") || rawName.equalsIgnoreCase("hikvision") ||
					rawName.equalsIgnoreCase("thermaltake") || rawName.equalsIgnoreCase("nzxt") || rawName.equalsIgnoreCase("seagate") ||
					rawName.equalsIgnoreCase("toshiba") || rawName.equalsIgnoreCase("westerndigital") || rawName.equalsIgnoreCase("fsp") ||
					rawName.equalsIgnoreCase("antec") || rawName.equalsIgnoreCase("razer") || rawName.equalsIgnoreCase("lianli") ||
					rawName.equalsIgnoreCase("deepcool") || rawName.equalsIgnoreCase("phanteks") || rawName.equalsIgnoreCase("coolermaster") ||
					rawName.equalsIgnoreCase("jonsbo") || rawName.equalsIgnoreCase("lg") || rawName.equalsIgnoreCase("dell") ||
					rawName.equalsIgnoreCase("sony") || rawName.equalsIgnoreCase("lenovo") || rawName.equalsIgnoreCase("northbayou") ||
					rawName.equalsIgnoreCase("humanmotion") || rawName.equalsIgnoreCase("noctua") || rawName.equalsIgnoreCase("idcooling")) {
					continue;
				}

				Optional<Product> existing = productRepository.findBySlug(slug);
				if (existing.isPresent()) {
					Product product = existing.get();
					if (product.getThumbnailUrl() == null || product.getThumbnailUrl().isBlank()) {
						String thumbnailUrl = uploadProductThumbnail(categoryName, rawName, imagePath.toFile());
						product.setThumbnailUrl(thumbnailUrl);
						productRepository.save(product);
					}
					// Update specs if empty
					if (product.getSpecsJson() == null || product.getSpecsJson().isEmpty() || product.getSpecsJson().size() <= 1) {
						product.setSpecsJson(buildSpecsJson(categoryName, rawName, matchBrand(rawName, brands)));
						productRepository.save(product);
					}
					continue;
				}

				Product product = new Product();
				product.setCategory(category);
				product.setBrand(matchBrand(rawName, brands));
				product.setName(displayName);
				product.setSlug(slug);
				product.setPrice(BigDecimal.ZERO);
				product.setStock(0);
				product.setDescription("Seeded from sample images");
				product.setSpecsJson(buildSpecsJson(categoryName, rawName, product.getBrand()));

				String thumbnailUrl = uploadProductThumbnail(categoryName, rawName, imagePath.toFile());
				product.setThumbnailUrl(thumbnailUrl);
				productRepository.save(product);
			}
		}
	}

	private String uploadProductThumbnail(String categoryName, String rawName, File file) {
		String publicId = slugify(rawName);
		String folder = String.format("%s/Product_thumbnails/%s", CLOUD_ROOT, categoryName);
		return mediaService.upload(file, folder, publicId);
	}

	private Brand matchBrand(String rawProductName, List<Brand> brands) {
		if (brands.isEmpty()) {
			return null;
		}
		String normalizedProduct = normalizeMatch(rawProductName);
		for (Brand brand : brands) {
			String normalizedBrand = normalizeMatch(brand.getName());
			if (!normalizedBrand.isBlank() && normalizedProduct.contains(normalizedBrand)) {
				return brand;
			}
		}
		return brands.get(0); // Default to first brand if no match found
	}

	private JsonNode buildSpecsJson(String categoryName, String rawName, Brand brand) {
		String componentType = mapComponentType(categoryName);
		Map<String, Object> specs = new HashMap<>();
		specs.put("component_type", componentType);
		
		String brandName = brand != null ? brand.getName() : "Unknown";
		String lowerName = rawName.toLowerCase(Locale.ROOT);

		switch (categoryName.toLowerCase(Locale.ROOT)) {
			case "cpu":
				buildCpuSpecs(specs, lowerName, brandName);
				break;
			case "vga":
				buildVgaSpecs(specs, lowerName, brandName);
				break;
			case "ram":
				buildRamSpecs(specs, lowerName, brandName);
				break;
			case "mainboard":
				buildMainboardSpecs(specs, lowerName, brandName);
				break;
			case "ssd":
				buildSsdSpecs(specs, lowerName, brandName);
				break;
			case "psu":
				buildPsuSpecs(specs, lowerName, brandName);
				break;
			case "case":
				buildCaseSpecs(specs, lowerName, brandName);
				break;
			case "monitor":
				buildMonitorSpecs(specs, lowerName, brandName);
				break;
			case "cooler":
				buildCoolerSpecs(specs, lowerName, brandName);
				break;
			case "fan":
				buildFanSpecs(specs, lowerName, brandName);
				break;
		}

		return objectMapper.valueToTree(specs);
	}

	private void buildCpuSpecs(Map<String, Object> specs, String name, String brand) {
		specs.put("brand", brand);
		if (name.contains("9000x")) {
			specs.put("series", "Ryzen 9");
			specs.put("socket", "AM5");
			specs.put("cores", 16);
			specs.put("threads", 32);
			specs.put("base_clock_ghz", 4.5);
			specs.put("boost_clock_ghz", 5.7);
			specs.put("cache_mb", 64);
			specs.put("tdp_w", 170);
			specs.put("integrated_gpu", true);
			specs.put("performance_score", 45000);
		} else if (name.contains("5900x")) {
			specs.put("series", "Ryzen 9");
			specs.put("socket", "AM4");
			specs.put("cores", 12);
			specs.put("threads", 24);
			specs.put("base_clock_ghz", 3.7);
			specs.put("boost_clock_ghz", 4.8);
			specs.put("cache_mb", 64);
			specs.put("tdp_w", 105);
			specs.put("integrated_gpu", false);
			specs.put("performance_score", 35000);
		} else if (name.contains("7500f")) {
			specs.put("series", "Ryzen 5");
			specs.put("socket", "AM5");
			specs.put("cores", 6);
			specs.put("threads", 12);
			specs.put("base_clock_ghz", 3.7);
			specs.put("boost_clock_ghz", 5.0);
			specs.put("cache_mb", 32);
			specs.put("tdp_w", 65);
			specs.put("integrated_gpu", false);
			specs.put("performance_score", 25000);
		} else if (name.contains("7800x3d")) {
			specs.put("series", "Ryzen 7");
			specs.put("socket", "AM5");
			specs.put("cores", 8);
			specs.put("threads", 16);
			specs.put("base_clock_ghz", 4.2);
			specs.put("boost_clock_ghz", 5.0);
			specs.put("cache_mb", 96);
			specs.put("tdp_w", 120);
			specs.put("integrated_gpu", true);
			specs.put("performance_score", 34000);
		} else if (name.contains("8500g")) {
			specs.put("series", "Ryzen 5");
			specs.put("socket", "AM5");
			specs.put("cores", 6);
			specs.put("threads", 12);
			specs.put("base_clock_ghz", 3.5);
			specs.put("boost_clock_ghz", 5.0);
			specs.put("cache_mb", 16);
			specs.put("tdp_w", 65);
			specs.put("integrated_gpu", true);
			specs.put("performance_score", 21000);
		}
	}

	private void buildVgaSpecs(Map<String, Object> specs, String name, String brand) {
		specs.put("brand", brand);
		if (name.contains("6600")) {
			specs.put("chipset", "RX 6600");
			specs.put("vram_gb", 8);
			specs.put("vram_type", "GDDR6");
			specs.put("base_clock_mhz", 1626);
			specs.put("boost_clock_mhz", 2491);
			specs.put("tdp_w", 132);
			specs.put("length_mm", 243);
			specs.put("min_psu_w", 500);
			specs.put("performance_score", 15000);
		} else if (name.contains("1650")) {
			specs.put("chipset", "GTX 1650 Super");
			specs.put("vram_gb", 4);
			specs.put("vram_type", "GDDR6");
			specs.put("base_clock_mhz", 1530);
			specs.put("boost_clock_mhz", 1755);
			specs.put("tdp_w", 100);
			specs.put("length_mm", 248);
			specs.put("min_psu_w", 350);
			specs.put("performance_score", 9000);
		} else if (name.contains("5070 ti")) {
			specs.put("chipset", "RTX 5070 Ti");
			specs.put("vram_gb", 16);
			specs.put("vram_type", "GDDR7");
			specs.put("base_clock_mhz", 2100);
			specs.put("boost_clock_mhz", 2600);
			specs.put("tdp_w", 285);
			specs.put("length_mm", 330);
			specs.put("min_psu_w", 750);
			specs.put("performance_score", 32000);
		} else if (name.contains("5060 ti")) {
			specs.put("chipset", "RTX 5060 Ti");
			specs.put("vram_gb", 8);
			specs.put("vram_type", "GDDR6");
			specs.put("base_clock_mhz", 2000);
			specs.put("boost_clock_mhz", 2500);
			specs.put("tdp_w", 160);
			specs.put("length_mm", 242);
			specs.put("min_psu_w", 550);
			specs.put("performance_score", 20000);
		} else if (name.contains("5050")) {
			specs.put("chipset", "RTX 5050");
			specs.put("vram_gb", 8);
			specs.put("vram_type", "GDDR6");
			specs.put("base_clock_mhz", 1900);
			specs.put("boost_clock_mhz", 2400);
			specs.put("tdp_w", 115);
			specs.put("length_mm", 200);
			specs.put("min_psu_w", 450);
			specs.put("performance_score", 12000);
		} else if (name.contains("b580")) {
			specs.put("chipset", "Arc B580");
			specs.put("vram_gb", 12);
			specs.put("vram_type", "GDDR6");
			specs.put("base_clock_mhz", 2000);
			specs.put("boost_clock_mhz", 2400);
			specs.put("tdp_w", 225);
			specs.put("length_mm", 280);
			specs.put("min_psu_w", 600);
			specs.put("performance_score", 18000);
		}
	}

	private void buildRamSpecs(Map<String, Object> specs, String name, String brand) {
		specs.put("brand", brand);
		if (name.contains("ddr4")) {
			specs.put("type", "DDR4");
			specs.put("capacity_gb", 16);
			specs.put("bus_speed_mhz", 3200);
			specs.put("kit", "1x16GB");
			specs.put("latency_cl", 16);
			specs.put("has_rgb", name.contains("rgb"));
		} else if (name.contains("pro rgb 32gb")) {
			specs.put("type", "DDR4");
			specs.put("capacity_gb", 32);
			specs.put("bus_speed_mhz", 3600);
			specs.put("kit", "2x16GB");
			specs.put("latency_cl", 18);
			specs.put("has_rgb", true);
		} else if (name.contains("royal")) {
			specs.put("type", "DDR4");
			specs.put("capacity_gb", 32);
			specs.put("bus_speed_mhz", 4000);
			specs.put("kit", "2x16GB");
			specs.put("latency_cl", 18);
			specs.put("has_rgb", true);
		} else if (name.contains("dominator")) {
			specs.put("type", "DDR5");
			specs.put("capacity_gb", 32);
			specs.put("bus_speed_mhz", 6000);
			specs.put("kit", "2x16GB");
			specs.put("latency_cl", 36);
			specs.put("has_rgb", true);
		} else if (name.contains("trident-z-rgb")) {
			specs.put("type", "DDR5");
			specs.put("capacity_gb", 32);
			specs.put("bus_speed_mhz", 5600);
			specs.put("kit", "2x16GB");
			specs.put("latency_cl", 36);
			specs.put("has_rgb", true);
		}
	}

	private void buildMainboardSpecs(Map<String, Object> specs, String name, String brand) {
		specs.put("brand", brand);
		if (name.contains("z790")) {
			specs.put("chipset", "Z790");
			specs.put("socket", "LGA1700");
			specs.put("form_factor", "ATX");
			specs.put("ram_slots", 4);
			specs.put("ram_type", "DDR5");
			specs.put("max_ram_gb", 192);
			specs.put("m2_slots", 4);
			specs.put("has_wifi", name.contains("wifi") || name.contains("hero"));
		} else if (name.contains("x870")) {
			specs.put("chipset", "X870");
			specs.put("socket", "AM5");
			specs.put("form_factor", "ATX");
			specs.put("ram_slots", 4);
			specs.put("ram_type", "DDR5");
			specs.put("max_ram_gb", 192);
			specs.put("m2_slots", 3);
			specs.put("has_wifi", true);
		} else if (name.contains("z690")) {
			specs.put("chipset", "Z690");
			specs.put("socket", "LGA1700");
			specs.put("form_factor", "ATX");
			specs.put("ram_slots", 4);
			specs.put("ram_type", "DDR5");
			specs.put("max_ram_gb", 128);
			specs.put("m2_slots", 4);
			specs.put("has_wifi", true);
		}
	}

	private void buildSsdSpecs(Map<String, Object> specs, String name, String brand) {
		specs.put("brand", brand);
		specs.put("type", "SSD");
		if (name.contains("snv3s_2000gb")) {
			specs.put("interface", "NVMe PCIe Gen4");
			specs.put("capacity_gb", 2000);
			specs.put("read_speed_mbps", 6000);
			specs.put("write_speed_mbps", 5000);
		} else if (name.contains("970evo_500gb")) {
			specs.put("interface", "NVMe PCIe Gen3");
			specs.put("capacity_gb", 500);
			specs.put("read_speed_mbps", 3500);
			specs.put("write_speed_mbps", 3300);
		} else if (name.contains("evo-plus-250gb")) {
			specs.put("interface", "NVMe PCIe Gen3");
			specs.put("capacity_gb", 250);
			specs.put("read_speed_mbps", 3500);
			specs.put("write_speed_mbps", 2300);
		} else if (name.contains("aorus_rgb_ssd_1tb")) {
			specs.put("interface", "NVMe PCIe Gen4");
			specs.put("capacity_gb", 1000);
			specs.put("read_speed_mbps", 5000);
			specs.put("write_speed_mbps", 4400);
		}
	}

	private void buildPsuSpecs(Map<String, Object> specs, String name, String brand) {
		specs.put("brand", brand);
		specs.put("form_factor", "ATX");
		if (name.contains("1000g")) {
			specs.put("wattage", 1000);
			specs.put("efficiency_rating", "80 Plus Gold");
			specs.put("modularity", "Full Modular");
		} else if (name.contains("platinum_modular") || name.contains("1300")) {
			specs.put("wattage", 1300);
			specs.put("efficiency_rating", "80 Plus Platinum");
			specs.put("modularity", "Full Modular");
		} else if (name.contains("rm750e")) {
			specs.put("wattage", 750);
			specs.put("efficiency_rating", "80 Plus Gold");
			specs.put("modularity", "Full Modular");
		}
	}

	private void buildCaseSpecs(Map<String, Object> specs, String name, String brand) {
		specs.put("brand", brand);
		if (name.contains("d32")) {
			specs.put("size", "Micro-ATX Tower");
			specs.put("supported_mainboards", List.of("M-ATX", "ITX"));
			specs.put("max_gpu_length_mm", 365);
			specs.put("max_cpu_cooler_height_mm", 164);
		} else if (name.contains("h9")) {
			specs.put("size", "Mid Tower");
			specs.put("supported_mainboards", List.of("ATX", "M-ATX", "ITX"));
			specs.put("max_gpu_length_mm", 435);
			specs.put("max_cpu_cooler_height_mm", 165);
		} else if (name.contains("vision")) {
			specs.put("size", "Mid Tower");
			specs.put("supported_mainboards", List.of("E-ATX", "ATX", "M-ATX", "ITX"));
			specs.put("max_gpu_length_mm", 455);
			specs.put("max_cpu_cooler_height_mm", 167);
		} else if (name.contains("mini")) {
			specs.put("size", "Mini Tower");
			specs.put("supported_mainboards", List.of("ATX", "M-ATX", "ITX"));
			specs.put("max_gpu_length_mm", 395);
			specs.put("max_cpu_cooler_height_mm", 170);
		} else if (name.contains("luca")) {
			specs.put("size", "Mid Tower");
			specs.put("supported_mainboards", List.of("E-ATX", "ATX", "M-ATX", "ITX"));
			specs.put("max_gpu_length_mm", 400);
			specs.put("max_cpu_cooler_height_mm", 170);
		}
	}

	private void buildMonitorSpecs(Map<String, Object> specs, String name, String brand) {
		specs.put("brand", brand);
		if (name.contains("27u411a")) {
			specs.put("size_inch", 27);
			specs.put("resolution", "3840x2160");
			specs.put("panel_type", "IPS");
			specs.put("refresh_rate_hz", 60);
			specs.put("response_time_ms", 5);
			specs.put("brightness_cdm2", 400);
			specs.put("aspect_ratio", "16:9");
			specs.put("color_accuracy", "99% sRGB");
			specs.put("has_hdr", true);
			specs.put("ports", List.of("HDMI", "DisplayPort", "Type-C"));
		} else if (name.contains("pg27aqwp") || name.contains("xg27acdms")) {
			specs.put("size_inch", 27);
			specs.put("resolution", "2560x1440");
			specs.put("panel_type", "OLED");
			specs.put("refresh_rate_hz", 240);
			specs.put("response_time_ms", 1);
			specs.put("brightness_cdm2", 1000);
			specs.put("aspect_ratio", "16:9");
			specs.put("color_accuracy", "99% DCI-P3");
			specs.put("has_hdr", true);
			specs.put("ports", List.of("HDMI 2.1", "DisplayPort 1.4"));
		} else if (name.contains("vy249hgr")) {
			specs.put("size_inch", 24);
			specs.put("resolution", "1920x1080");
			specs.put("panel_type", "IPS");
			specs.put("refresh_rate_hz", 120);
			specs.put("response_time_ms", 1);
			specs.put("brightness_cdm2", 250);
			specs.put("aspect_ratio", "16:9");
			specs.put("color_accuracy", "99% sRGB");
			specs.put("has_hdr", false);
			specs.put("ports", List.of("HDMI", "VGA"));
		} else if (name.contains("pg34wcdn")) {
			specs.put("size_inch", 34);
			specs.put("resolution", "3440x1440");
			specs.put("panel_type", "OLED");
			specs.put("refresh_rate_hz", 360);
			specs.put("response_time_ms", 1);
			specs.put("brightness_cdm2", 1000);
			specs.put("aspect_ratio", "21:9");
			specs.put("color_accuracy", "99% DCI-P3");
			specs.put("has_hdr", true);
			specs.put("ports", List.of("HDMI 2.1", "DisplayPort 1.4", "USB-C"));
		} else if (name.contains("xg27aqdng")) {
			specs.put("size_inch", 27);
			specs.put("resolution", "2560x1440");
			specs.put("panel_type", "OLED");
			specs.put("refresh_rate_hz", 360);
			specs.put("response_time_ms", 1);
			specs.put("brightness_cdm2", 1000);
			specs.put("aspect_ratio", "16:9");
			specs.put("color_accuracy", "99% DCI-P3");
			specs.put("has_hdr", true);
			specs.put("ports", List.of("HDMI 2.1", "DisplayPort 1.4"));
		}
	}

	private void buildCoolerSpecs(Map<String, Object> specs, String name, String brand) {
		specs.put("brand", brand);
		if (name.contains("hyper_620s")) {
			specs.put("type", "Air Cooling");
			specs.put("supported_sockets", List.of("LGA1700", "AM5", "AM4"));
			specs.put("tdp_rating_w", 200);
			specs.put("fan_size_mm", 120);
			specs.put("height_mm", 154);
			specs.put("has_rgb", true);
			specs.put("noise_level_db", 27);
		} else if (name.contains("panorama-360")) {
			specs.put("type", "Liquid Cooling");
			specs.put("supported_sockets", List.of("LGA1700", "AM5", "AM4"));
			specs.put("tdp_rating_w", 300);
			specs.put("fan_size_mm", 120);
			specs.put("radiator_size_mm", 360);
			specs.put("has_rgb", true);
			specs.put("noise_level_db", 30);
		} else if (name.contains("th-240")) {
			specs.put("type", "Liquid Cooling");
			specs.put("supported_sockets", List.of("LGA1700", "AM5", "AM4"));
			specs.put("tdp_rating_w", 250);
			specs.put("fan_size_mm", 120);
			specs.put("radiator_size_mm", 240);
			specs.put("has_rgb", true);
			specs.put("noise_level_db", 28);
		} else if (name.contains("ak400")) {
			specs.put("type", "Air Cooling");
			specs.put("supported_sockets", List.of("LGA1700", "AM5", "AM4"));
			specs.put("tdp_rating_w", 220);
			specs.put("fan_size_mm", 120);
			specs.put("height_mm", 155);
			specs.put("has_rgb", true);
			specs.put("noise_level_db", 28);
		} else if (name.contains("hydroshift")) {
			specs.put("type", "Liquid Cooling");
			specs.put("supported_sockets", List.of("LGA1700", "AM5"));
			specs.put("tdp_rating_w", 300);
			specs.put("fan_size_mm", 120);
			specs.put("radiator_size_mm", 360);
			specs.put("has_rgb", true);
			specs.put("noise_level_db", 30);
		}
	}

	private void buildFanSpecs(Map<String, Object> specs, String name, String brand) {
		specs.put("brand", brand);
		specs.put("size_mm", 120);
		if (name.contains("rs120")) {
			specs.put("fan_speed_rpm", 2100);
			specs.put("airflow_cfm", 72.8);
			specs.put("noise_level_db", 36);
			specs.put("bearing_type", "Magnetic Dome");
			specs.put("connection_type", "4-pin PWM");
			specs.put("has_rgb", true);
			specs.put("is_addressable_rgb", true);
		} else if (name.contains("eflow-120")) {
			specs.put("fan_speed_rpm", 1500);
			specs.put("airflow_cfm", 50.0);
			specs.put("noise_level_db", 25);
			specs.put("bearing_type", "Hydraulic");
			specs.put("connection_type", "4-pin PWM");
			specs.put("has_rgb", true);
			specs.put("is_addressable_rgb", true);
		}
	}

	private String mapComponentType(String categoryName) {
		String key = categoryName.trim().toLowerCase(Locale.ROOT);
		switch (key) {
			case "vga":
				return "GPU";
			case "ssd":
				return "STORAGE";
			case "mainboard":
				return "MAINBOARD";
			case "psu":
				return "PSU";
			case "cpu":
				return "CPU";
			case "ram":
				return "RAM";
			case "case":
				return "CASE";
			case "cooler":
				return "COOLER";
			case "fan":
				return "FAN";
			case "monitor":
				return "MONITOR";
			default:
				return categoryName.toUpperCase(Locale.ROOT);
		}
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

	private String normalizeMatch(String input) {
		return input.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
	}

	private String normalizeKey(String input) {
		return input == null ? "" : input.trim().toLowerCase(Locale.ROOT);
	}
}


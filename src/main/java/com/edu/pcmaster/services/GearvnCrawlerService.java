package com.edu.pcmaster.services;

import com.edu.pcmaster.common.exception.BadRequestException;
import com.edu.pcmaster.common.exception.ResourceNotFoundException;
import com.edu.pcmaster.dto.product.GearvnImportRequest;
import com.edu.pcmaster.dto.product.ProductResponse;
import com.edu.pcmaster.models.Brand;
import com.edu.pcmaster.models.Category;
import com.edu.pcmaster.models.Product;
import com.edu.pcmaster.repositories.BrandRepository;
import com.edu.pcmaster.repositories.CategoryRepository;
import com.edu.pcmaster.repositories.ProductRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.edu.pcmaster.models.ProductImage;
import com.edu.pcmaster.repositories.ProductImageRepository;
import com.edu.pcmaster.services.MediaService;

@Service
public class GearvnCrawlerService {

	private static final Logger log = LoggerFactory.getLogger(GearvnCrawlerService.class);

	private static final Map<String, String> LABEL_TRANSLATIONS = new HashMap<>();
	static {
		LABEL_TRANSLATIONS.put("thương hiệu", "brand");
		LABEL_TRANSLATIONS.put("bảo hành", "warranty");
		LABEL_TRANSLATIONS.put("dòng sản phẩm", "product_series");
		LABEL_TRANSLATIONS.put("dòng vga", "vga_series");
		LABEL_TRANSLATIONS.put("số nhân cuda cores (nvidia)", "cuda_cores");
		LABEL_TRANSLATIONS.put("số nhân cuda cores", "cuda_cores");
		LABEL_TRANSLATIONS.put("ai tops", "ai_tops");
		LABEL_TRANSLATIONS.put("xung nhịp gpu base", "base_clock");
		LABEL_TRANSLATIONS.put("xung nhịp gpu boost", "boost_clock");
		LABEL_TRANSLATIONS.put("bộ nhớ (vram)", "vram");
		LABEL_TRANSLATIONS.put("bộ nhớ vram", "vram");
		LABEL_TRANSLATIONS.put("bus bộ nhớ", "memory_bus");
		LABEL_TRANSLATIONS.put("kiểu bộ nhớ", "memory_type");
		LABEL_TRANSLATIONS.put("chuẩn giao tiếp", "interface");
		LABEL_TRANSLATIONS.put("tản nhiệt nước", "water_cooled");
		LABEL_TRANSLATIONS.put("kích thước radiator tản nhiệt", "radiator_dimensions");
		LABEL_TRANSLATIONS.put("kích thước radiator", "radiator_dimensions");
		LABEL_TRANSLATIONS.put("số quạt tản nhiệt", "fan_count");
		LABEL_TRANSLATIONS.put("cổng kết nối", "ports");
		LABEL_TRANSLATIONS.put("directx hỗ trợ", "directx");
		LABEL_TRANSLATIONS.put("hỗ trợ dlss", "dlss");
		LABEL_TRANSLATIONS.put("hỗ trợ ray tracing", "ray_tracing");
		LABEL_TRANSLATIONS.put("opengl hỗ trợ", "opengl");
		LABEL_TRANSLATIONS.put("nhân đồ họa", "graphics_processor");
		LABEL_TRANSLATIONS.put("hỗ trợ đa màn hình", "multi_monitor");
		LABEL_TRANSLATIONS.put("số slot chiếm dụng", "slot_width");
		LABEL_TRANSLATIONS.put("độ phân giải tối đa", "max_resolution");
		LABEL_TRANSLATIONS.put("đầu cấp nguồn", "power_connectors");
		LABEL_TRANSLATIONS.put("nguồn đề xuất", "recommended_psu");
		LABEL_TRANSLATIONS.put("tdp", "tdp");
		LABEL_TRANSLATIONS.put("kích thước card", "dimensions");
		LABEL_TRANSLATIONS.put("phụ kiện đi kèm", "accessories");
		LABEL_TRANSLATIONS.put("số nhân", "cores");
		LABEL_TRANSLATIONS.put("số luồng", "threads");
		LABEL_TRANSLATIONS.put("xung nhịp cơ bản", "base_clock");
		LABEL_TRANSLATIONS.put("xung nhịp tối đa", "boost_clock");
		LABEL_TRANSLATIONS.put("tiến trình", "lithography");
		LABEL_TRANSLATIONS.put("socket", "socket");
		LABEL_TRANSLATIONS.put("hỗ trợ ram", "memory_support");
		LABEL_TRANSLATIONS.put("dung lượng", "capacity_gb");
		LABEL_TRANSLATIONS.put("capacity", "capacity_gb");
		LABEL_TRANSLATIONS.put("bus ram", "bus_speed_mhz");
		LABEL_TRANSLATIONS.put("tốc độ ram", "bus_speed_mhz");
		LABEL_TRANSLATIONS.put("loại ram", "ram_type");
		LABEL_TRANSLATIONS.put("lo_i_ram", "ram_type");
		LABEL_TRANSLATIONS.put("rgb_led", "has_rgb");
		LABEL_TRANSLATIONS.put("đèn led rgb", "has_rgb");
		LABEL_TRANSLATIONS.put("cas_latency", "latency_cl");
		LABEL_TRANSLATIONS.put("độ trễ lat", "latency_cl");
		LABEL_TRANSLATIONS.put("chuẩn mainboard", "form_factor");
		LABEL_TRANSLATIONS.put("chipset", "chipset");
		LABEL_TRANSLATIONS.put("khe cắm ram", "ram_slots");
		LABEL_TRANSLATIONS.put("dung lượng ram tối đa", "max_ram_gb");
		LABEL_TRANSLATIONS.put("hãng sản xuất", "brand");
		LABEL_TRANSLATIONS.put("nhà sản xuất", "brand");
		LABEL_TRANSLATIONS.put("trọng lượng", "weight");
		LABEL_TRANSLATIONS.put("cân nặng", "weight");

		// PSU translations
		LABEL_TRANSLATIONS.put("chu_n_ch_ng_nh_n", "efficiency_rating");
		LABEL_TRANSLATIONS.put("chuẩn chứng nhận", "efficiency_rating");
		LABEL_TRANSLATIONS.put("hi_u_su_t", "efficiency_rating");
		LABEL_TRANSLATIONS.put("hiệu suất", "efficiency_rating");
		LABEL_TRANSLATIONS.put("lo_i_modular", "modularity");
		LABEL_TRANSLATIONS.put("loại modular", "modularity");
		LABEL_TRANSLATIONS.put("chu_n_ngu_n", "form_factor");
		LABEL_TRANSLATIONS.put("chuẩn nguồn", "form_factor");
		LABEL_TRANSLATIONS.put("c_ng_su_t", "wattage");
		LABEL_TRANSLATIONS.put("công suất", "wattage");
		LABEL_TRANSLATIONS.put("c_ng_su_t_t_i_a", "wattage");
		LABEL_TRANSLATIONS.put("công suất tối đa", "wattage");

		// CPU translations
		LABEL_TRANSLATIONS.put("dòng cpu", "series");
		LABEL_TRANSLATIONS.put("d_ng_cpu", "series");
		LABEL_TRANSLATIONS.put("thế hệ cpu", "generation");
		LABEL_TRANSLATIONS.put("th_h_cpu", "generation");
		LABEL_TRANSLATIONS.put("kiến trúc", "architecture");
		LABEL_TRANSLATIONS.put("ki_n_tr_c", "architecture");
		LABEL_TRANSLATIONS.put("platform", "platform");
		LABEL_TRANSLATIONS.put("bộ nhớ đệm l1 cache", "l1_cache");
		LABEL_TRANSLATIONS.put("b_nh_m_l1_cache", "l1_cache");
		LABEL_TRANSLATIONS.put("bộ nhớ đệm l2 cache", "l2_cache");
		LABEL_TRANSLATIONS.put("b_nh_m_l2_cache", "l2_cache");
		LABEL_TRANSLATIONS.put("bộ nhớ đệm l3 cache", "l3_cache");
		LABEL_TRANSLATIONS.put("b_nh_m_l3_cache", "l3_cache");
		LABEL_TRANSLATIONS.put("xung nhịp cơ bản (p-core base)", "base_clock_ghz");
		LABEL_TRANSLATIONS.put("xung_nh_p_c_b_n_p_core_base", "base_clock_ghz");
		LABEL_TRANSLATIONS.put("xung nhịp tối đa (p-core turbo)", "boost_clock_ghz");
		LABEL_TRANSLATIONS.put("xung_nh_p_t_i_a_p_core_turbo", "boost_clock_ghz");
		LABEL_TRANSLATIONS.put("đồ họa tích hợp", "integrated_gpu");
		LABEL_TRANSLATIONS.put("h_a_t_ch_h_p", "integrated_gpu");
		LABEL_TRANSLATIONS.put("chip đồ họa tích hợp", "gpu_integrated_name");
		LABEL_TRANSLATIONS.put("chip_h_a_t_ch_h_p", "gpu_integrated_name");
		LABEL_TRANSLATIONS.put("hỗ trợ pcie", "pcie_support");
		LABEL_TRANSLATIONS.put("h_tr_pcie", "pcie_support");
		LABEL_TRANSLATIONS.put("hỗ trợ loại ram", "memory_support");
		LABEL_TRANSLATIONS.put("h_tr_lo_i_ram", "memory_support");
		LABEL_TRANSLATIONS.put("chất liệu vỏ / mặt dưới", "material");
		LABEL_TRANSLATIONS.put("chất liệu vỏ mặt dưới", "material");
		LABEL_TRANSLATIONS.put("ch_t_li_u_v_m_t_d_i", "material");
		LABEL_TRANSLATIONS.put("số kênh ram", "memory_channels");
		LABEL_TRANSLATIONS.put("s_k_nh_ram", "memory_channels");
		LABEL_TRANSLATIONS.put("tdp (điện năng tiêu thụ)", "tdp_w");
		LABEL_TRANSLATIONS.put("tdp_i_n_n_ng_ti_u_th", "tdp_w");
		LABEL_TRANSLATIONS.put("tốc độ ram tối đa", "memory_speed");
		LABEL_TRANSLATIONS.put("t_c_ram_t_i_a", "memory_speed");
		LABEL_TRANSLATIONS.put("tình trạng", "condition");
		LABEL_TRANSLATIONS.put("t_nh_tr_ng", "condition");
		LABEL_TRANSLATIONS.put("warranty", "warranty");

		// P-core and E-core mappings
		LABEL_TRANSLATIONS.put("số nhân p-core", "p_cores");
		LABEL_TRANSLATIONS.put("s_nh_n_p_core", "p_cores");
		LABEL_TRANSLATIONS.put("số nhân e-core", "e_cores");
		LABEL_TRANSLATIONS.put("s_nh_n_e_core", "e_cores");
		LABEL_TRANSLATIONS.put("xung nhịp cơ bản (e-core base)", "e_core_base_clock_ghz");
		LABEL_TRANSLATIONS.put("xung_nh_p_c_b_n_e_core_base", "e_core_base_clock_ghz");
		LABEL_TRANSLATIONS.put("xung nhịp tối đa (e-core turbo)", "e_core_boost_clock_ghz");
		LABEL_TRANSLATIONS.put("xung_nh_p_t_i_a_e_core_turbo", "e_core_boost_clock_ghz");
		LABEL_TRANSLATIONS.put("tdp max (điện năng tiêu thụ tối đa)", "tdp_w");
		LABEL_TRANSLATIONS.put("tdp_max_i_n_n_ng_ti_u_th_t_i_a", "tdp_w");

		// Mainboard translations
		LABEL_TRANSLATIONS.put("ki_u_ram_h_tr", "ram_type");
		LABEL_TRANSLATIONS.put("k_ch_th_c", "form_factor");
		LABEL_TRANSLATIONS.put("s_khe_m_2", "m2_slots");
		LABEL_TRANSLATIONS.put("khe_ram_t_i_a", "ram_slots");
		LABEL_TRANSLATIONS.put("max_memory_capacity", "max_ram_gb");
		LABEL_TRANSLATIONS.put("wi_fi", "has_wifi");
		LABEL_TRANSLATIONS.put("wifi", "has_wifi");
	}

	private final ProductRepository productRepository;
	private final BrandRepository brandRepository;
	private final CategoryRepository categoryRepository;
	private final ObjectMapper objectMapper;
	private final ProductService productService;
	private final MediaService mediaService;
	private final ProductImageRepository productImageRepository;

	public GearvnCrawlerService(ProductRepository productRepository,
								BrandRepository brandRepository,
								CategoryRepository categoryRepository,
								ObjectMapper objectMapper,
								ProductService productService,
								MediaService mediaService,
								ProductImageRepository productImageRepository) {
		this.productRepository = productRepository;
		this.brandRepository = brandRepository;
		this.categoryRepository = categoryRepository;
		this.objectMapper = objectMapper;
		this.productService = productService;
		this.mediaService = mediaService;
		this.productImageRepository = productImageRepository;
	}

	/**
	 * Crawls GearVN page to preview data without saving.
	 */
	public Map<String, Object> previewProduct(GearvnImportRequest request) {
		return crawlData(request.url(), request.categoryId());
	}

	/**
	 * Crawls GearVN page, maps to Product entity, and saves to database.
	 */
	@Transactional
	public ProductResponse importProduct(GearvnImportRequest request) {
		Map<String, Object> crawled = crawlData(request.url(), request.categoryId());

		// 1. Category check
		Category category = categoryRepository.findById(request.categoryId())
				.orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy danh mục ID: " + request.categoryId()));

		@SuppressWarnings("unchecked")
		Map<String, String> specsMap = (Map<String, String>) crawled.get("specs");

		// 2. Brand check (find or create)
		String brandName = (String) crawled.get("brand");
		if (specsMap != null && specsMap.containsKey("brand") && !specsMap.get("brand").trim().isEmpty()) {
			brandName = specsMap.get("brand").trim();
		}

		String finalBrandName = brandName;
		Brand brand = brandRepository.findByNameIgnoreCase(finalBrandName)
				.orElseGet(() -> {
					Brand newBrand = new Brand();
					newBrand.setName(finalBrandName);
					log.info("Auto-creating brand for GearVN import: {}", finalBrandName);
					return brandRepository.save(newBrand);
				});

		// 3. Map details
		String title = (String) crawled.get("title");
		BigDecimal price = (BigDecimal) crawled.get("price");
		String thumbnailUrl = (String) crawled.get("thumbnailUrl");

		// Generate Slug first to use for Cloudinary folder path
		String slug = generateSlug(title);
		if (productRepository.findBySlug(slug).isPresent()) {
			slug = slug + "_" + System.currentTimeMillis();
		}

		// Upload main thumbnail to Cloudinary
		String cloudFolder = "PCMAster_Storage/Product_detail_Image/" + slug;
		String cloudThumbnailUrl = null;
		if (thumbnailUrl != null && !thumbnailUrl.isEmpty()) {
			cloudThumbnailUrl = uploadToCloudinary(thumbnailUrl, cloudFolder, "thumbnail");
		}

		ObjectNode specsJson;
		try {
			// Ensure brand and component_type are set in specs JSON
			Map<String, Object> extendedSpecs = new LinkedHashMap<>(specsMap);
			extendedSpecs.put("brand", brand.getName());
			extendedSpecs.put("component_type", getComponentTypeFromCategory(category));
			specsJson = objectMapper.valueToTree(extendedSpecs);
		} catch (Exception e) {
			specsJson = objectMapper.createObjectNode();
		}

		// Create Product entity
		Product product = new Product();
		product.setName(title);
		product.setSlug(slug);
		product.setBrand(brand);
		product.setCategory(category);
		product.setPrice(price);
		product.setStock(0); // Admin adds stock later through purchase order
		product.setDescription(null); // Ignore description, do not map to object
		product.setThumbnailUrl(cloudThumbnailUrl != null ? cloudThumbnailUrl : thumbnailUrl);
		product.setSpecsJson(specsJson);

		Product saved = productRepository.save(product);

		// 4. Scrape & upload gallery images to Cloudinary, then save to ProductImage table
		@SuppressWarnings("unchecked")
		List<String> galleryImages = (List<String>) crawled.get("images");
		if (galleryImages != null && !galleryImages.isEmpty()) {
			int index = 1;
			for (String imgUrl : galleryImages) {
				String publicId = "image_" + index;
				String uploadedUrl = uploadToCloudinary(imgUrl, cloudFolder, publicId);
				if (uploadedUrl != null) {
					ProductImage productImage = new ProductImage();
					productImage.setProduct(saved);
					productImage.setUrl(uploadedUrl);
					productImageRepository.save(productImage);
				}
				index++;
			}
		}

		Map<Long, Integer> discountsMap = productService.getActiveProductDiscountsMap();
		return buildProductResponse(saved, discountsMap);
	}

	// ──────────────────────────────────────────────────────────────────────────
	//  Crawling & Parsing Helpers
	// ──────────────────────────────────────────────────────────────────────────

	private Map<String, Object> crawlData(String url, Long categoryId) {
		try {
			log.info("Crawling GearVN URL: {}", url);
			Category category = null;
			if (categoryId != null) {
				category = categoryRepository.findById(categoryId).orElse(null);
			}
			Document doc = Jsoup.connect(url)
					.userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
					.timeout(15000)
					.get();

			String html = doc.html();

			// 1. Extract product data JSON using regex
			Pattern dataPattern = Pattern.compile("\"?data\"?\\s*:\\s*(\\{.+?\\})\\s*,\\s*\"?id\"?");
			Matcher dataMatcher = dataPattern.matcher(html);

			if (!dataMatcher.find()) {
				throw new BadRequestException("Không thể tìm thấy block dữ liệu sản phẩm trong trang GearVN. Vui lòng kiểm tra lại URL.");
			}

			String jsonString = dataMatcher.group(1);
			JsonNode productData = objectMapper.readTree(jsonString);

			// Extract basic details
			String title = productData.path("title").asText("").trim();
			String vendorBrand = productData.path("vendor").asText("").trim();
			String description = productData.path("description").asText("").trim();
			double rawPrice = productData.path("price").asDouble(0.0);
			BigDecimal price = BigDecimal.valueOf(rawPrice / 100.0); // Convert Haravan cents to VND

			// Extract thumbnail URL
			String featuredImage = productData.path("featured_image").asText("").trim();
			String thumbnailUrl = ensureHttpsUrl(featuredImage);

			// Extract SKU
			String sku = "";
			JsonNode variants = productData.path("variants");
			if (variants.isArray() && !variants.isEmpty()) {
				sku = variants.get(0).path("sku").asText("").trim();
			}

			// Extract all gallery images
			List<String> images = new ArrayList<>();
			JsonNode imagesNode = productData.path("images");
			if (imagesNode.isArray()) {
				for (JsonNode imgNode : imagesNode) {
					String imgUrl = imgNode.asText("").trim();
					if (!imgUrl.isEmpty()) {
						images.add(ensureHttpsUrl(imgUrl));
					}
				}
			}

			// Crawl additional images from the description HTML and push to Cloudinary
			if (!description.isEmpty()) {
				try {
					Document descDoc = Jsoup.parse(description);
					Elements descImgs = descDoc.select("img");
					for (Element img : descImgs) {
						String src = img.attr("src").trim();
						if (!src.isEmpty()) {
							String absSrc = ensureHttpsUrl(src);
							if (!images.contains(absSrc)) {
								images.add(absSrc);
							}
						}
					}
				} catch (Exception e) {
					log.warn("Failed to parse description images", e);
				}
			}

			// 2. Fetch technical specifications from worker
			Map<String, String> specs = new LinkedHashMap<>();
			if (!sku.isEmpty()) {
				try {
					specs = fetchSpecsFromWorker(sku, category);
				} catch (Exception e) {
					log.warn("Failed to fetch specs from GearVN worker for SKU {}: {}", sku, e.getMessage());
				}
			}

			Map<String, Object> result = new HashMap<>();
			result.put("title", title);
			result.put("brand", vendorBrand.isEmpty() ? "GearVN" : vendorBrand);
			result.put("description", null); // Bỏ qua phần mô tả, không map vào object
			result.put("price", price);
			result.put("thumbnailUrl", thumbnailUrl);
			result.put("sku", sku);
			result.put("specs", specs);
			result.put("images", images);

			return result;

		} catch (BadRequestException e) {
			throw e;
		} catch (Exception e) {
			log.error("Error crawling GearVN URL {}", url, e);
			throw new BadRequestException("Lỗi khi crawl dữ liệu từ GearVN: " + e.getMessage());
		}
	}

	private Map<String, String> fetchSpecsFromWorker(String sku, Category category) {
		Map<String, String> specs = new LinkedHashMap<>();
		String workerUrl = "https://cdp-embed-worker.cloud-gearvn.workers.dev/v1/js/product-specs?sku=" + sku + "&group_types=core";
		
		try {
			log.info("Fetching specs from Cloudflare worker: {}", workerUrl);
			Connection.Response response = Jsoup.connect(workerUrl)
					.userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
					.timeout(10000)
					.ignoreContentType(true)
					.execute();

			String body = response.body();

			// Extract table inner HTML
			Pattern tablePattern = Pattern.compile("container\\.innerHTML\\s*=\\s*'([\\s\\S]+?)';");
			Matcher matcher = tablePattern.matcher(body);

			if (matcher.find()) {
				String escapedHtml = matcher.group(1);
				// Unescape quotes and slashes
				String htmlTable = escapedHtml.replace("\\\"", "\"").replace("\\/", "/");
				Document tableDoc = Jsoup.parse(htmlTable);
				Elements rows = tableDoc.select("tr.gvn-spec-row");

				String componentType = category != null ? getComponentTypeFromCategory(category) : "OTHER";

				for (Element row : rows) {
					Element th = row.selectFirst("th");
					Element td = row.selectFirst("td");
					if (th != null && td != null) {
						String key = cleanString(th.text());
						String val = cleanString(td.text());
						if (!key.isEmpty() && !val.isEmpty()) {
							String translatedKey = translateKey(key, componentType);
							specs.put(translatedKey, normalizeValue(translatedKey, val));
						}
					}
				}
			}
		} catch (Exception e) {
			log.warn("Specs worker fetch failed for {}: {}", sku, e.getMessage());
		}

		return specs;
	}

	private String uploadToCloudinary(String imageUrl, String folder, String publicId) {
		try {
			if (imageUrl == null || imageUrl.isEmpty()) return null;
			byte[] bytes = Jsoup.connect(imageUrl)
					.ignoreContentType(true)
					.maxBodySize(20 * 1024 * 1024)
					.userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
					.timeout(15000)
					.execute()
					.bodyAsBytes();
			return mediaService.upload(bytes, folder, publicId);
		} catch (Exception e) {
			log.warn("Failed to download or upload image to Cloudinary: {}", imageUrl, e);
			return null;
		}
	}

	private String ensureHttpsUrl(String url) {
		if (url == null || url.isEmpty()) return "";
		if (url.startsWith("//")) {
			return "https:" + url;
		}
		return url;
	}

	private String translateKey(String key, String componentType) {
		String keyLower = key.toLowerCase().trim();

		// Custom translation mapping for PSU to map "form factor" to "dimensions"
		if ("PSU".equalsIgnoreCase(componentType)) {
			String snake = toSnakeCase(keyLower);
			if (snake.equals("form_factor") || snake.equals("form_factor_psu")) {
				return "dimensions";
			}
		}

		String translation = LABEL_TRANSLATIONS.get(keyLower);
		if (translation != null) {
			return translation;
		}

		// Fallback to snake_case key lookup to handle NFD/NFC encoding mismatches
		String snakeKey = toSnakeCase(key);
		translation = LABEL_TRANSLATIONS.get(snakeKey);
		if (translation != null) {
			return translation;
		}

		return snakeKey;
	}

	private String normalizeValue(String key, String val) {
		if (val == null) return "";
		String valTrimmed = val.trim();
		String valLower = valTrimmed.toLowerCase();

		// Normalize boolean values
		if (valLower.equals("có") || valLower.equals("yes") || valLower.equals("true")) {
			return "true";
		}
		if (valLower.equals("không") || valLower.equals("no") || valLower.equals("false")) {
			return "false";
		}

		// WiFi detection for has_wifi
		if (key.equals("has_wifi")) {
			if (valLower.equals("không") || valLower.equals("no") || valLower.equals("false") || valLower.isEmpty()) {
				return "false";
			}
			return "true";
		}

		// Clean up duplicate units or suffixes (e.g. MB MB, KB KB, GB GB)
		valTrimmed = valTrimmed.replaceAll("(?i)\\b(mb|kb|gb)\\b\\s+\\b\\1\\b", "$1");

		// Clean numeric suffixes for specific fields to match type constraints
		if (key.equals("cores") || key.equals("threads") || key.equals("p_cores") || key.equals("e_cores")
				|| key.contains("slots") || key.contains("sata") || key.contains("usb") || key.contains("ports")
				|| key.contains("cores") || key.contains("fan") || key.contains("count")) {
			valTrimmed = valTrimmed.replaceAll("(?i)\\s*(cores|threads| nhân| luồng|khe|cổng| slots| slot| ram| quạt| quat)", "");
		}
		if (key.equals("base_clock_ghz") || key.equals("boost_clock_ghz") || key.contains("clock")) {
			valTrimmed = valTrimmed.replaceAll("(?i)\\s*(ghz|mhz| ghz| mhz)", "");
		}
		if (key.equals("tdp_w") || key.contains("tdp") || key.contains("psu") || key.contains("wattage")) {
			valTrimmed = valTrimmed.replaceAll("(?i)\\s*(w| w)", "");
		}
		if (key.equals("max_ram_gb") || key.equals("max_memory_capacity") || key.equals("capacity_gb") || key.equals("capacity")) {
			valTrimmed = valTrimmed.replaceAll("(?i)\\s*(gb|tb| gb| tb)", "");
		}
		if (key.equals("latency_cl") || key.equals("latency") || key.equals("cas_latency")) {
			valTrimmed = valTrimmed.replaceAll("(?i)\\s*(cl| cl)", "");
		}
		if (key.equals("chipset")) {
			valTrimmed = valTrimmed.replaceAll("(?i)\\b(amd|intel)\\b\\s*", "");
		}
		if (key.contains("weight")) {
			valTrimmed = valTrimmed.replaceAll("(?i)\\s*(kg|g| kg| g)", "");
		}
		if (key.contains("bus")) {
			valTrimmed = valTrimmed.replaceAll("(?i)\\s*(bit|b| bit| b)", "");
		}

		return valTrimmed;
	}

	private String toSnakeCase(String text) {
		return text.toLowerCase()
				.replaceAll("[^a-z0-9]+", "_")
				.replaceAll("^_+|_+$", "");
	}

	private String cleanString(String text) {
		if (text == null) return "";
		String normalized = Normalizer.normalize(text, Normalizer.Form.NFC);
		normalized = normalized.replaceAll("[\\s\\h\\xa0\\u2007\\u202F\\u00A0]+", " ");
		return normalized.trim();
	}

	private String generateSlug(String text) {
		if (text == null || text.isEmpty()) return "product";
		return Normalizer.normalize(text, Normalizer.Form.NFD)
				.replaceAll("[\\p{InCombiningDiacriticalMarks}]", "")
				.replaceAll("[đĐ]", "d")
				.toLowerCase()
				.replaceAll("[^a-z0-9\\s]", "")
				.trim()
				.replaceAll("\\s+", "_");
	}

	private String getComponentTypeFromCategory(Category category) {
		if (category.getSlug() == null) return "OTHER";
		String slug = category.getSlug().toLowerCase().replace("-", "");
		if (slug.contains("vga") || slug.contains("graphic") || slug.contains("video")) return "GPU";
		if (slug.contains("ram") || slug.contains("memory")) return "RAM";
		if (slug.contains("psu") || slug.contains("power") || slug.contains("nguon")) return "PSU";
		if (slug.contains("mainboard") || slug.contains("mother")) return "MAINBOARD";
		if (slug.contains("cpu") || slug.contains("processor")) return "CPU";
		if (slug.contains("cooler") || slug.contains("tannhiet")) return "COOLER";
		if (slug.contains("fan") || slug.contains("quat")) return "FAN";
		if (slug.contains("ssd") || slug.contains("hdd") || slug.contains("storage")) return "STORAGE";
		if (slug.contains("monitor") || slug.contains("manhinh")) return "MONITOR";
		if (slug.contains("case") || slug.contains("vomay")) return "CASE";
		return "OTHER";
	}

	private ProductResponse buildProductResponse(Product product, Map<Long, Integer> discountsMap) {
		Integer discountPercent = discountsMap.get(product.getId());
		BigDecimal discountPrice = discountPercent != null
				? product.getPrice().subtract(product.getPrice().multiply(BigDecimal.valueOf(discountPercent)).divide(BigDecimal.valueOf(100)))
				: null;

		com.edu.pcmaster.dto.category.CategoryResponse catResp = null;
		if (product.getCategory() != null) {
			catResp = new com.edu.pcmaster.dto.category.CategoryResponse(
					product.getCategory().getId(),
					product.getCategory().getName(),
					product.getCategory().getSlug(),
					product.getCategory().getParent() == null ? null : product.getCategory().getParent().getId()
			);
		}

		com.edu.pcmaster.dto.brand.BrandResponse brandResp = null;
		if (product.getBrand() != null) {
			brandResp = new com.edu.pcmaster.dto.brand.BrandResponse(
					product.getBrand().getId(),
					product.getBrand().getName(),
					product.getBrand().getLogoUrl()
			);
		}

		return new ProductResponse(
				product.getId(),
				product.getCategory() == null ? null : product.getCategory().getId(),
				product.getBrand() == null ? null : product.getBrand().getId(),
				catResp,
				brandResp,
				product.getName(),
				product.getSlug(),
				product.getPrice(),
				discountPrice,
				discountPercent,
				product.getStock(),
				product.getThumbnailUrl(),
				product.getDescription(),
				product.getSpecsJson() == null ? null : product.getSpecsJson().toString(),
				product.getCreatedAt(),
				product.getUpdatedAt(),
				null
		);
	}
}

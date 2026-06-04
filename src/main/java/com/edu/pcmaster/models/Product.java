package com.edu.pcmaster.models;

import java.math.BigDecimal;
import java.time.Instant;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "products")
@Getter
@Setter
public class Product {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "category_id")
	private Category category;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "brand_id")
	private Brand brand;

	@Column(nullable = false, length = 255)
	private String name;

	@Column(nullable = false, unique = true, length = 255)
	private String slug;

	@Column(nullable = false, precision = 12, scale = 2)
	private BigDecimal price;

	@Column(nullable = false)
	private Integer stock = 0;

	@Column(name = "thumbnail_url")
	private String thumbnailUrl;

	@Column(columnDefinition = "text")
	private String description;

	@Column(name = "specs", columnDefinition = "jsonb")
	@JdbcTypeCode(SqlTypes.JSON)
	private JsonNode specsJson;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	@OneToOne(mappedBy = "product", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	@com.fasterxml.jackson.annotation.JsonIgnore
	private PcSystemDetail pcSystemDetail;

	@ManyToMany(mappedBy = "products", fetch = FetchType.LAZY)
	@com.fasterxml.jackson.annotation.JsonIgnore
	private List<Promotion> promotions = new ArrayList<>();

	@PrePersist
	void onCreate() {
		Instant now = Instant.now();
		createdAt = now;
		updatedAt = now;
		normalizeSpecs();
	}

	@PreUpdate
	void onUpdate() {
		updatedAt = Instant.now();
		normalizeSpecs();
	}















	// Function phục vụ crawl data, không ảnh hưởng logic
	private void normalizeSpecs() {
		if (specsJson == null || !specsJson.isObject()) {
			specsJson = JsonNodeFactory.instance.objectNode();
			return;
		}

		ObjectNode objectNode = (ObjectNode) specsJson;

		// Determine component type
		String componentType = "";
		if (objectNode.has("component_type")) {
			componentType = objectNode.get("component_type").asText();
		} else if (category != null && category.getSlug() != null) {
			String slug = category.getSlug().toLowerCase().replace("-", "");
			if (slug.contains("psu") || slug.contains("power") || slug.contains("nguon")) {
				componentType = "PSU";
			} else if (slug.contains("ram") || slug.contains("memory")) {
				componentType = "RAM";
			} else if (slug.contains("mainboard") || slug.contains("mother") || slug.contains("board")) {
				componentType = "MAINBOARD";
			} else if (slug.contains("vga") || slug.contains("gpu") || slug.contains("graphic")) {
				componentType = "GPU";
			} else if (slug.contains("cpu") || slug.contains("processor") || slug.contains("vi-xu-ly")) {
				componentType = "CPU";
			} else if (slug.contains("ssd") || slug.contains("hdd") || slug.contains("storage") || slug.contains("o-cung")) {
				componentType = "STORAGE";
			}
		}

		if ("PSU".equalsIgnoreCase(componentType)) {
			// Normalize keys
			java.util.Map<String, String> psuMappings = new java.util.HashMap<>();
			psuMappings.put("chu_n_ch_ng_nh_n", "efficiency_rating");
			psuMappings.put("chuẩn chứng nhận", "efficiency_rating");
			psuMappings.put("efficiency_rating", "efficiency_rating");
			psuMappings.put("lo_i_modular", "modularity");
			psuMappings.put("loại modular", "modularity");
			psuMappings.put("modularity", "modularity");
			psuMappings.put("chu_n_ngu_n", "form_factor");
			psuMappings.put("chuẩn nguồn", "form_factor");
			psuMappings.put("form_factor", "form_factor");
			psuMappings.put("c_ng_su_t", "wattage");
			psuMappings.put("công suất", "wattage");
			psuMappings.put("c_ng_su_t_t_i_a", "wattage");
			psuMappings.put("công suất tối đa", "wattage");
			psuMappings.put("wattage", "wattage");

			psuMappings.put("ch_qu_t", "fan_mode");
			psuMappings.put("m_u_s_c", "color");
			psuMappings.put("hi_u_su_t", "efficiency_percent");
			psuMappings.put("ki_u_rail", "rail_type");
			psuMappings.put("s_c_ng_c_m", "connectors");
			psuMappings.put("i_n_p_u_v_o", "input_voltage");
			psuMappings.put("k_ch_th_c_qu_t", "fan_size_mm");
			psuMappings.put("ki_u_d_y_ngu_n", "cable_type");
			psuMappings.put("phi_n_b_n_chu_n", "atx_version");
			psuMappings.put("t_nh_n_ng_b_o_v", "protection_features");
			psuMappings.put("t_c_quay_c_a_fan", "fan_speed");
			psuMappings.put("t_nh_n_ng_c_bi_t", "special_features");

			java.util.List<String> keys = new java.util.ArrayList<>();
			objectNode.fieldNames().forEachRemaining(keys::add);

			for (String key : keys) {
				String cleanKey = toSnakeCase(key);
				String standardKey = psuMappings.get(cleanKey);
				if (standardKey != null) {
					JsonNode valueNode = objectNode.get(key);
					if (!objectNode.has(standardKey) || !key.equals(standardKey)) {
						objectNode.set(standardKey, valueNode);
					}
					if (!key.equals(standardKey)) {
						objectNode.remove(key);
					}
				}
			}

			// Value normalizations
			if (objectNode.has("wattage")) {
				JsonNode wattageNode = objectNode.get("wattage");
				if (wattageNode.isTextual()) {
					String rawVal = wattageNode.asText();
					String cleanVal = rawVal.toLowerCase().replaceAll("(?i)\\s*[wW]\\b", "").replaceAll("\\s*w/g", "").trim();
					try {
						int wattVal = Integer.parseInt(cleanVal);
						objectNode.put("wattage", wattVal);
					} catch (NumberFormatException e) {
						objectNode.put("wattage", cleanVal);
					}
				}
			}

			if (objectNode.has("efficiency_percent")) {
				JsonNode effNode = objectNode.get("efficiency_percent");
				if (effNode.isTextual()) {
					String rawVal = effNode.asText();
					String cleanVal = rawVal.toLowerCase().replaceAll("[^0-9.]", "").trim();
					try {
						double effVal = Double.parseDouble(cleanVal);
						objectNode.put("efficiency_percent", effVal);
					} catch (NumberFormatException e) {
						objectNode.put("efficiency_percent", cleanVal);
					}
				}
			}

			if (objectNode.has("fan_size_mm")) {
				JsonNode fsNode = objectNode.get("fan_size_mm");
				if (fsNode.isTextual()) {
					String rawVal = fsNode.asText();
					String cleanVal = rawVal.toLowerCase().replaceAll("[^0-9]", "").trim();
					try {
						int fsVal = Integer.parseInt(cleanVal);
						objectNode.put("fan_size_mm", fsVal);
					} catch (NumberFormatException e) {
						objectNode.put("fan_size_mm", cleanVal);
					}
				}
			}
		} else if ("RAM".equalsIgnoreCase(componentType)) {
			// Normalize keys
			java.util.Map<String, String> ramMappings = new java.util.HashMap<>();
			ramMappings.put("type", "ram_type");
			ramMappings.put("lo_i_ram", "ram_type");
			ramMappings.put("loại ram", "ram_type");
			ramMappings.put("ram_type", "ram_type");

			ramMappings.put("cas_latency", "latency_cl");
			ramMappings.put("cas_latency_cl", "latency_cl");
			ramMappings.put("độ trễ lat", "latency_cl");
			ramMappings.put("latency_cl", "latency_cl");

			ramMappings.put("capacity", "capacity_gb");
			ramMappings.put("dung lượng", "capacity_gb");
			ramMappings.put("capacity_gb", "capacity_gb");

			ramMappings.put("memory_speed", "bus_speed_mhz");
			ramMappings.put("tốc độ ram", "bus_speed_mhz");
			ramMappings.put("bus ram", "bus_speed_mhz");
			ramMappings.put("bus_speed_mhz", "bus_speed_mhz");

			ramMappings.put("rgb_led", "has_rgb");
			ramMappings.put("đèn led rgb", "has_rgb");
			ramMappings.put("has_rgb", "has_rgb");

			ramMappings.put("i_n_p", "voltage");
			ramMappings.put("voltage", "voltage");
			ramMappings.put("s_k_nh", "channels");
			ramMappings.put("channels", "channels");
			ramMappings.put("lo_i_m_y", "device_type");
			ramMappings.put("device_type", "device_type");
			ramMappings.put("t_n_nhi_t", "has_heatsink");
			ramMappings.put("has_heatsink", "has_heatsink");
			ramMappings.put("b_ng_th_ng", "bandwidth_gbps");
			ramMappings.put("bandwidth_gbps", "bandwidth_gbps");
			ramMappings.put("s_l_ng_thanh", "module_count");
			ramMappings.put("module_count", "module_count");

			java.util.List<String> keys = new java.util.ArrayList<>();
			objectNode.fieldNames().forEachRemaining(keys::add);

			for (String key : keys) {
				String cleanKey = toSnakeCase(key);
				String standardKey = ramMappings.get(cleanKey);
				if (standardKey != null) {
					JsonNode valueNode = objectNode.get(key);
					if (!objectNode.has(standardKey) || !key.equals(standardKey)) {
						objectNode.set(standardKey, valueNode);
					}
					if (!key.equals(standardKey)) {
						objectNode.remove(key);
					}
				}
			}

			// Value normalizations
			if (objectNode.has("capacity_gb")) {
				JsonNode capNode = objectNode.get("capacity_gb");
				if (capNode.isTextual()) {
					String rawVal = capNode.asText();
					String cleanVal = rawVal.toLowerCase().replaceAll("(?i)\\s*(gb|tb)\\b", "").trim();
					try {
						int capVal = Integer.parseInt(cleanVal);
						objectNode.put("capacity_gb", capVal);
					} catch (NumberFormatException e) {
						objectNode.put("capacity_gb", cleanVal);
					}
				}
			} else {
				// Try to extract capacity from name
				String name = this.name;
				if (name != null) {
					java.util.regex.Pattern p = java.util.regex.Pattern.compile("(?i)\\b(\\d+)\\s*gb\\b");
					java.util.regex.Matcher m = p.matcher(name);
					if (m.find()) {
						try {
							int capVal = Integer.parseInt(m.group(1));
							objectNode.put("capacity_gb", capVal);
						} catch (NumberFormatException ignored) {}
					}
				}
			}

			if (objectNode.has("bus_speed_mhz")) {
				JsonNode busNode = objectNode.get("bus_speed_mhz");
				if (busNode.isTextual()) {
					String rawVal = busNode.asText();
					String cleanVal = rawVal.toLowerCase().replaceAll("(?i)\\s*(mhz|ghz)\\b", "").trim();
					try {
						int busVal = Integer.parseInt(cleanVal);
						objectNode.put("bus_speed_mhz", busVal);
					} catch (NumberFormatException e) {
						objectNode.put("bus_speed_mhz", cleanVal);
					}
				}
			} else {
				// Try to extract bus speed from name
				String name = this.name;
				if (name != null) {
					java.util.regex.Pattern p = java.util.regex.Pattern.compile("(?i)(2400|3000|3200|3600|4000|4800|5200|5600|6000|6600)\\s*(?:mhz)?");
					java.util.regex.Matcher m = p.matcher(name);
					if (m.find()) {
						try {
							int busVal = Integer.parseInt(m.group(1));
							objectNode.put("bus_speed_mhz", busVal);
						} catch (NumberFormatException ignored) {}
					}
				}
			}

			if (objectNode.has("latency_cl")) {
				JsonNode latNode = objectNode.get("latency_cl");
				if (latNode.isTextual()) {
					String rawVal = latNode.asText();
					String cleanVal = rawVal.toLowerCase().replaceAll("(?i)\\s*cl\\b", "").trim();
					try {
						int latVal = Integer.parseInt(cleanVal);
						objectNode.put("latency_cl", latVal);
					} catch (NumberFormatException e) {
						objectNode.put("latency_cl", cleanVal);
					}
				}
			} else {
				// Try to extract latency CL from name
				String name = this.name;
				if (name != null) {
					java.util.regex.Pattern p = java.util.regex.Pattern.compile("(?i)\\bcl(\\d+)\\b|\\bc(\\d+)\\b");
					java.util.regex.Matcher m = p.matcher(name);
					if (m.find()) {
						String val = m.group(1) != null ? m.group(1) : m.group(2);
						try {
							int latVal = Integer.parseInt(val);
							objectNode.put("latency_cl", latVal);
						} catch (NumberFormatException ignored) {}
					}
				}
			}

			if (objectNode.has("ram_type")) {
				String rt = objectNode.get("ram_type").asText();
				if (rt.toUpperCase().contains("DDR5")) {
					objectNode.put("ram_type", "DDR5");
				} else if (rt.toUpperCase().contains("DDR4")) {
					objectNode.put("ram_type", "DDR4");
				}
			} else {
				// Try to extract ram_type from name
				String name = this.name;
				if (name != null) {
					if (name.toUpperCase().contains("DDR5")) {
						objectNode.put("ram_type", "DDR5");
					} else if (name.toUpperCase().contains("DDR4")) {
						objectNode.put("ram_type", "DDR4");
					}
				}
			}

			if (objectNode.has("has_rgb")) {
				JsonNode rgbNode = objectNode.get("has_rgb");
				if (rgbNode.isTextual()) {
					String rawVal = rgbNode.asText().toLowerCase().trim();
					if (rawVal.equals("có") || rawVal.equals("yes") || rawVal.equals("true")) {
						objectNode.put("has_rgb", true);
					} else if (rawVal.equals("không") || rawVal.equals("no") || rawVal.equals("false")) {
						objectNode.put("has_rgb", false);
					}
				}
			} else {
				// Check name for RGB
				String name = this.name;
				if (name != null && name.toUpperCase().contains("RGB")) {
					objectNode.put("has_rgb", true);
				} else {
					objectNode.put("has_rgb", false);
				}
			}

			if (objectNode.has("voltage")) {
				JsonNode inpNode = objectNode.get("voltage");
				if (inpNode.isTextual()) {
					String rawVal = inpNode.asText();
					String cleanVal = rawVal.toLowerCase().replaceAll("(?i)\\s*v\\b", "").trim();
					try {
						double inpVal = Double.parseDouble(cleanVal);
						objectNode.put("voltage", inpVal);
					} catch (NumberFormatException e) {
						objectNode.put("voltage", cleanVal);
					}
				}
			}

			if (objectNode.has("channels")) {
				String rawVal = objectNode.get("channels").asText().toLowerCase();
				if (rawVal.contains("đôi") || rawVal.contains("2x")) {
					objectNode.put("channels", "Kênh đôi");
				} else if (rawVal.contains("đơn") || rawVal.contains("1x")) {
					objectNode.put("channels", "Kênh đơn");
				}
			}

			if (objectNode.has("device_type")) {
				String rawVal = objectNode.get("device_type").asText().toLowerCase();
				if (rawVal.contains("để bàn") || rawVal.contains("pc")) {
					objectNode.put("device_type", "Máy tính để bàn");
				} else if (rawVal.contains("máy chủ") || rawVal.contains("workstation")) {
					objectNode.put("device_type", "Máy chủ / Máy trạm");
				}
			}

			if (objectNode.has("has_heatsink")) {
				JsonNode tnNode = objectNode.get("has_heatsink");
				if (tnNode.isTextual()) {
					String rawVal = tnNode.asText().toLowerCase().trim();
					if (rawVal.equals("có") || rawVal.equals("yes") || rawVal.equals("true")) {
						objectNode.put("has_heatsink", true);
					} else if (rawVal.equals("không") || rawVal.equals("no") || rawVal.equals("false")) {
						objectNode.put("has_heatsink", false);
					}
				}
			}

			if (objectNode.has("bandwidth_gbps")) {
				JsonNode btNode = objectNode.get("bandwidth_gbps");
				if (btNode.isTextual()) {
					String rawVal = btNode.asText();
					String cleanVal = rawVal.toLowerCase().replaceAll("(?i)\\s*gb/s\\b", "").trim();
					try {
						double btVal = Double.parseDouble(cleanVal);
						objectNode.put("bandwidth_gbps", btVal);
					} catch (NumberFormatException e) {
						objectNode.put("bandwidth_gbps", cleanVal);
					}
				}
			}

			if (objectNode.has("module_count")) {
				JsonNode sltNode = objectNode.get("module_count");
				if (sltNode.isTextual()) {
					String rawVal = sltNode.asText();
					String cleanVal = rawVal.toLowerCase().replaceAll("(?i)\\s*(thanh|modules|module|pcs)\\b", "").trim();
					try {
						int sltVal = Integer.parseInt(cleanVal);
						objectNode.put("module_count", sltVal);
					} catch (NumberFormatException e) {
						objectNode.put("module_count", cleanVal);
					}
				}
			}
		} else if ("MAINBOARD".equalsIgnoreCase(componentType)) {
			// Normalize keys
			java.util.Map<String, String> mbMappings = new java.util.HashMap<>();
			mbMappings.put("max_memory_capacity", "max_ram_gb");
			mbMappings.put("dung_l_ng_ram_t_i_a", "max_ram_gb");
			mbMappings.put("max_ram_gb", "max_ram_gb");

			mbMappings.put("ram_slots", "ram_slots");
			mbMappings.put("khe_c_m_ram", "ram_slots");
			mbMappings.put("khe_ram_t_i_a", "ram_slots");

			mbMappings.put("m2_slots", "m2_slots");
			mbMappings.put("s_khe_m_2", "m2_slots");
			mbMappings.put("s_khe_m2", "m2_slots");

			mbMappings.put("wi_fi", "has_wifi");
			mbMappings.put("wifi", "has_wifi");
			mbMappings.put("has_wifi", "has_wifi");

			mbMappings.put("ki_u_ram_h_tr", "ram_type");
			mbMappings.put("ram_type", "ram_type");

			mbMappings.put("k_ch_th_c", "form_factor");
			mbMappings.put("chu_n_mainboard", "form_factor");
			mbMappings.put("form_factor", "form_factor");

			mbMappings.put("s_c_ng_sata", "sata_ports");
			mbMappings.put("sata_ports", "sata_ports");
			mbMappings.put("cpu_h_tr", "cpu_support");
			mbMappings.put("cpu_support", "cpu_support");
			mbMappings.put("k_t_n_i_m_ng_lan", "lan_speed");
			mbMappings.put("lan_speed", "lan_speed");
			mbMappings.put("c_ng_usb", "usb_ports");
			mbMappings.put("usb_ports", "usb_ports");
			mbMappings.put("c_ng_xu_t_h_nh", "display_outputs");
			mbMappings.put("display_outputs", "display_outputs");
			mbMappings.put("c_ng_usb_type_c", "has_usb_type_c");
			mbMappings.put("has_usb_type_c", "has_usb_type_c");
			mbMappings.put("rgb_led", "has_rgb");
			mbMappings.put("has_rgb", "has_rgb");
			mbMappings.put("vrm_pha", "vrm_phases");
			mbMappings.put("vrm_phases", "vrm_phases");
			mbMappings.put("pcie_gen", "pcie_generation");
			mbMappings.put("pcie_generation", "pcie_generation");
			mbMappings.put("bluetooth", "has_bluetooth");
			mbMappings.put("has_bluetooth", "has_bluetooth");

			java.util.List<String> keys = new java.util.ArrayList<>();
			objectNode.fieldNames().forEachRemaining(keys::add);

			for (String key : keys) {
				String cleanKey = toSnakeCase(key);
				String standardKey = mbMappings.get(cleanKey);
				if (standardKey != null) {
					JsonNode valueNode = objectNode.get(key);
					if (!objectNode.has(standardKey) || !key.equals(standardKey)) {
						objectNode.set(standardKey, valueNode);
					}
					if (!key.equals(standardKey)) {
						objectNode.remove(key);
					}
				}
			}

			// Remove garbage key
			objectNode.remove("ch_t_li_u_v_m_t_tr_n");

			// Value normalizations
			if (objectNode.has("max_ram_gb")) {
				JsonNode maxRamNode = objectNode.get("max_ram_gb");
				if (maxRamNode.isTextual()) {
					String rawVal = maxRamNode.asText();
					String cleanVal = rawVal.toLowerCase().replaceAll("(?i)\\s*(gb|tb)\\b", "").trim();
					try {
						int maxRamVal = Integer.parseInt(cleanVal);
						objectNode.put("max_ram_gb", maxRamVal);
					} catch (NumberFormatException e) {
						objectNode.put("max_ram_gb", cleanVal);
					}
				}
			}

			if (objectNode.has("ram_slots")) {
				JsonNode slotsNode = objectNode.get("ram_slots");
				if (slotsNode.isTextual()) {
					String rawVal = slotsNode.asText();
					String cleanVal = rawVal.toLowerCase().replaceAll("(?i)\\s*(khe|cổng|slots|slot)\\b", "").trim();
					try {
						int slotsVal = Integer.parseInt(cleanVal);
						objectNode.put("ram_slots", slotsVal);
					} catch (NumberFormatException e) {
						objectNode.put("ram_slots", cleanVal);
					}
				}
			}

			if (objectNode.has("m2_slots")) {
				JsonNode m2Node = objectNode.get("m2_slots");
				if (m2Node.isTextual()) {
					String rawVal = m2Node.asText();
					String cleanVal = rawVal.toLowerCase().replaceAll("(?i)\\s*(khe|cổng|slots|slot)\\b", "").trim();
					try {
						int m2Val = Integer.parseInt(cleanVal);
						objectNode.put("m2_slots", m2Val);
					} catch (NumberFormatException e) {
						objectNode.put("m2_slots", cleanVal);
					}
				}
			}

			if (objectNode.has("ram_type")) {
				String rt = objectNode.get("ram_type").asText();
				if (rt.toUpperCase().contains("DDR5")) {
					objectNode.put("ram_type", "DDR5");
				} else if (rt.toUpperCase().contains("DDR4")) {
					objectNode.put("ram_type", "DDR4");
				}
			} else {
				// Try to extract from name
				String name = this.name;
				if (name != null) {
					if (name.toUpperCase().contains("DDR5")) {
						objectNode.put("ram_type", "DDR5");
					} else if (name.toUpperCase().contains("DDR4")) {
						objectNode.put("ram_type", "DDR4");
					}
				}
			}

			if (objectNode.has("has_wifi")) {
				JsonNode wifiNode = objectNode.get("has_wifi");
				if (wifiNode.isTextual()) {
					String rawVal = wifiNode.asText().toLowerCase().trim();
					if (rawVal.equals("có") || rawVal.equals("yes") || rawVal.equals("true") || rawVal.contains("wi-fi") || rawVal.contains("wifi")) {
						objectNode.put("has_wifi", true);
					} else if (rawVal.equals("không") || rawVal.equals("no") || rawVal.equals("false")) {
						objectNode.put("has_wifi", false);
					}
				}
			} else {
				// Try to detect wifi from name
				String name = this.name;
				if (name != null && (name.toUpperCase().contains("WIFI") || name.toUpperCase().contains("WI-FI") || name.toUpperCase().contains("WIFI7"))) {
					objectNode.put("has_wifi", true);
				} else {
					objectNode.put("has_wifi", false);
				}
			}

			if (objectNode.has("usb_ports")) {
				JsonNode usbNode = objectNode.get("usb_ports");
				if (usbNode.isTextual()) {
					String rawVal = usbNode.asText();
					String cleanVal = rawVal.toLowerCase().replaceAll("(?i)\\s*(cổng|ports|port)\\b", "").trim();
					try {
						int usbVal = Integer.parseInt(cleanVal);
						objectNode.put("usb_ports", usbVal);
					} catch (NumberFormatException e) {
						objectNode.put("usb_ports", cleanVal);
					}
				}
			}

			if (objectNode.has("sata_ports")) {
				JsonNode sataNode = objectNode.get("sata_ports");
				if (sataNode.isTextual()) {
					String rawVal = sataNode.asText();
					String cleanVal = rawVal.toLowerCase().replaceAll("(?i)\\s*(cổng|ports|port|sata)\\b", "").trim();
					try {
						int sataVal = Integer.parseInt(cleanVal);
						objectNode.put("sata_ports", sataVal);
					} catch (NumberFormatException e) {
						objectNode.put("sata_ports", cleanVal);
					}
				}
			}

			if (objectNode.has("has_usb_type_c")) {
				JsonNode utcNode = objectNode.get("has_usb_type_c");
				if (utcNode.isTextual()) {
					String rawVal = utcNode.asText().toLowerCase().trim();
					if (rawVal.equals("có") || rawVal.equals("yes") || rawVal.equals("true")) {
						objectNode.put("has_usb_type_c", true);
					} else if (rawVal.equals("không") || rawVal.equals("no") || rawVal.equals("false")) {
						objectNode.put("has_usb_type_c", false);
					}
				}
			}

			if (objectNode.has("lan_speed")) {
				JsonNode lanNode = objectNode.get("lan_speed");
				if (lanNode.isTextual()) {
					String rawVal = lanNode.asText();
					String cleanVal = rawVal.toLowerCase().replaceAll("(?i)\\s*gb/s\\b", "").trim();
					cleanVal = cleanVal.replaceAll("(?i)(gbps|mbps)", " $1");
					objectNode.put("lan_speed", cleanVal.toUpperCase().trim());
				}
			}

			if (objectNode.has("has_rgb")) {
				JsonNode rgbNode = objectNode.get("has_rgb");
				if (rgbNode.isTextual()) {
					String rawVal = rgbNode.asText().toLowerCase().trim();
					if (rawVal.equals("có") || rawVal.equals("yes") || rawVal.equals("true")) {
						objectNode.put("has_rgb", true);
					} else if (rawVal.equals("không") || rawVal.equals("no") || rawVal.equals("false")) {
						objectNode.put("has_rgb", false);
					}
				}
			}

			if (objectNode.has("has_bluetooth")) {
				JsonNode btNode = objectNode.get("has_bluetooth");
				if (btNode.isTextual()) {
					String rawVal = btNode.asText().toLowerCase().trim();
					if (rawVal.equals("có") || rawVal.equals("yes") || rawVal.equals("true") || rawVal.contains("bluetooth")) {
						objectNode.put("has_bluetooth", true);
					} else if (rawVal.equals("không") || rawVal.equals("no") || rawVal.equals("false")) {
						objectNode.put("has_bluetooth", false);
					}
				} else if (btNode.isBoolean()) {
					// keep as is
				} else {
					objectNode.put("has_bluetooth", true);
				}
			}
		} else if ("GPU".equalsIgnoreCase(componentType)) {
			// Normalize keys
			java.util.Map<String, String> gpuMappings = new java.util.HashMap<>();
			gpuMappings.put("tdp", "tdp_w");
			gpuMappings.put("tdp_w", "tdp_w");
			gpuMappings.put("vram", "vram_gb");
			gpuMappings.put("vram_gb", "vram_gb");
			gpuMappings.put("recommended_psu", "min_psu_w");
			gpuMappings.put("min_psu_w", "min_psu_w");
			gpuMappings.put("base_clock", "base_clock_mhz");
			gpuMappings.put("base_clock_mhz", "base_clock_mhz");
			gpuMappings.put("boost_clock", "boost_clock_mhz");
			gpuMappings.put("boost_clock_mhz", "boost_clock_mhz");
			gpuMappings.put("memory_type", "vram_type");
			gpuMappings.put("vram_type", "vram_type");
			gpuMappings.put("memory_bus", "memory_bus_bits");
			gpuMappings.put("memory_bus_bits", "memory_bus_bits");
			gpuMappings.put("fan_count", "fan_count");
			gpuMappings.put("cuda_cores", "cuda_cores");
			gpuMappings.put("slot_width", "slot_width");
			gpuMappings.put("s_nh_n_stream_processors_amd", "stream_processors");
			gpuMappings.put("stream_processors", "stream_processors");
			gpuMappings.put("ray_tracing", "ray_tracing");
			gpuMappings.put("water_cooled", "water_cooled");

			java.util.List<String> keys = new java.util.ArrayList<>();
			objectNode.fieldNames().forEachRemaining(keys::add);

			for (String key : keys) {
				String cleanKey = toSnakeCase(key);
				String standardKey = gpuMappings.get(cleanKey);
				if (standardKey != null) {
					JsonNode valueNode = objectNode.get(key);
					if (!objectNode.has(standardKey) || !key.equals(standardKey)) {
						objectNode.set(standardKey, valueNode);
					}
					if (!key.equals(standardKey)) {
						objectNode.remove(key);
					}
				}
			}

			// Value normalizations
			if (objectNode.has("tdp_w")) {
				JsonNode valNode = objectNode.get("tdp_w");
				if (valNode.isTextual()) {
					String rawVal = valNode.asText();
					String cleanVal = rawVal.toLowerCase().replaceAll("[^0-9]", "").trim();
					try {
						objectNode.put("tdp_w", Integer.parseInt(cleanVal));
					} catch (NumberFormatException ignored) {}
				}
			}

			if (objectNode.has("vram_gb")) {
				JsonNode valNode = objectNode.get("vram_gb");
				if (valNode.isTextual()) {
					String rawVal = valNode.asText();
					String cleanVal = rawVal.toLowerCase().replaceAll("[^0-9]", "").trim();
					try {
						objectNode.put("vram_gb", Integer.parseInt(cleanVal));
					} catch (NumberFormatException ignored) {}
				}
			}

			if (objectNode.has("min_psu_w")) {
				JsonNode valNode = objectNode.get("min_psu_w");
				if (valNode.isTextual()) {
					String rawVal = valNode.asText();
					String cleanVal = rawVal.toLowerCase().replaceAll("[^0-9]", "").trim();
					try {
						objectNode.put("min_psu_w", Integer.parseInt(cleanVal));
					} catch (NumberFormatException ignored) {}
				}
			}

			if (objectNode.has("base_clock_mhz")) {
				JsonNode valNode = objectNode.get("base_clock_mhz");
				if (valNode.isTextual()) {
					String rawVal = valNode.asText();
					String cleanVal = rawVal.toLowerCase().replaceAll("[^0-9]", "").trim();
					try {
						objectNode.put("base_clock_mhz", Integer.parseInt(cleanVal));
					} catch (NumberFormatException ignored) {}
				}
			}

			if (objectNode.has("boost_clock_mhz")) {
				JsonNode valNode = objectNode.get("boost_clock_mhz");
				if (valNode.isTextual()) {
					String rawVal = valNode.asText();
					String cleanVal = rawVal.toLowerCase().replaceAll("[^0-9]", "").trim();
					try {
						objectNode.put("boost_clock_mhz", Integer.parseInt(cleanVal));
					} catch (NumberFormatException ignored) {}
				}
			}

			if (objectNode.has("memory_bus_bits")) {
				JsonNode valNode = objectNode.get("memory_bus_bits");
				if (valNode.isTextual()) {
					String rawVal = valNode.asText();
					String cleanVal = rawVal.toLowerCase().replaceAll("[^0-9]", "").trim();
					try {
						objectNode.put("memory_bus_bits", Integer.parseInt(cleanVal));
					} catch (NumberFormatException ignored) {}
				}
			}

			if (objectNode.has("fan_count")) {
				JsonNode valNode = objectNode.get("fan_count");
				if (valNode.isTextual()) {
					String rawVal = valNode.asText();
					String cleanVal = rawVal.toLowerCase().replaceAll("[^0-9]", "").trim();
					try {
						objectNode.put("fan_count", Integer.parseInt(cleanVal));
					} catch (NumberFormatException ignored) {}
				}
			}

			if (objectNode.has("cuda_cores")) {
				JsonNode valNode = objectNode.get("cuda_cores");
				if (valNode.isTextual()) {
					String rawVal = valNode.asText();
					String cleanVal = rawVal.toLowerCase().replaceAll("[^0-9]", "").trim();
					try {
						objectNode.put("cuda_cores", Integer.parseInt(cleanVal));
					} catch (NumberFormatException ignored) {}
				}
			}

			if (objectNode.has("slot_width")) {
				JsonNode valNode = objectNode.get("slot_width");
				if (valNode.isTextual()) {
					String rawVal = valNode.asText();
					String cleanVal = rawVal.toLowerCase().replaceAll("[^0-9]", "").trim();
					try {
						objectNode.put("slot_width", Integer.parseInt(cleanVal));
					} catch (NumberFormatException ignored) {}
				}
			}

			if (objectNode.has("stream_processors")) {
				JsonNode valNode = objectNode.get("stream_processors");
				if (valNode.isTextual()) {
					String rawVal = valNode.asText();
					String cleanVal = rawVal.toLowerCase().replaceAll("[^0-9]", "").trim();
					try {
						objectNode.put("stream_processors", Integer.parseInt(cleanVal));
					} catch (NumberFormatException ignored) {}
				}
			}

			if (objectNode.has("dimensions")) {
				String dims = objectNode.get("dimensions").asText();
				String cleanDims = dims.toLowerCase().replaceAll("[xX×]", "x");
				String[] parts = cleanDims.split("x");
				if (parts.length > 0) {
					String lengthPart = parts[0].replaceAll("[^0-9]", "").trim();
					try {
						objectNode.put("length_mm", Integer.parseInt(lengthPart));
					} catch (NumberFormatException ignored) {}
				}
			}

			if (objectNode.has("ray_tracing")) {
				JsonNode rtNode = objectNode.get("ray_tracing");
				if (rtNode.isTextual()) {
					String rawVal = rtNode.asText().toLowerCase().trim();
					if (rawVal.equals("có") || rawVal.equals("yes") || rawVal.equals("true")) {
						objectNode.put("ray_tracing", true);
					} else if (rawVal.equals("không") || rawVal.equals("no") || rawVal.equals("false")) {
						objectNode.put("ray_tracing", false);
					}
				}
			}

			if (objectNode.has("water_cooled")) {
				JsonNode wcNode = objectNode.get("water_cooled");
				if (wcNode.isTextual()) {
					String rawVal = wcNode.asText().toLowerCase().trim();
					if (rawVal.equals("có") || rawVal.equals("yes") || rawVal.equals("true")) {
						objectNode.put("water_cooled", true);
					} else if (rawVal.equals("không") || rawVal.equals("no") || rawVal.equals("false")) {
						objectNode.put("water_cooled", false);
					}
				}
			}
		} else if ("CPU".equalsIgnoreCase(componentType)) {
			// Value normalizations for CPU numeric types to prevent string representations
			if (objectNode.has("cores")) {
				JsonNode valNode = objectNode.get("cores");
				if (valNode.isTextual()) {
					try {
						objectNode.put("cores", Integer.parseInt(valNode.asText().trim()));
					} catch (NumberFormatException ignored) {}
				}
			}
			if (objectNode.has("threads")) {
				JsonNode valNode = objectNode.get("threads");
				if (valNode.isTextual()) {
					try {
						objectNode.put("threads", Integer.parseInt(valNode.asText().trim()));
					} catch (NumberFormatException ignored) {}
				}
			}
			if (objectNode.has("tdp_w")) {
				JsonNode valNode = objectNode.get("tdp_w");
				if (valNode.isTextual()) {
					try {
						objectNode.put("tdp_w", Integer.parseInt(valNode.asText().replaceAll("[^0-9]", "").trim()));
					} catch (NumberFormatException ignored) {}
				}
			}
			if (objectNode.has("base_clock_ghz")) {
				JsonNode valNode = objectNode.get("base_clock_ghz");
				if (valNode.isTextual()) {
					try {
						objectNode.put("base_clock_ghz", Double.parseDouble(valNode.asText().trim()));
					} catch (NumberFormatException ignored) {}
				}
			}
			if (objectNode.has("boost_clock_ghz")) {
				JsonNode valNode = objectNode.get("boost_clock_ghz");
				if (valNode.isTextual()) {
					try {
						objectNode.put("boost_clock_ghz", Double.parseDouble(valNode.asText().trim()));
					} catch (NumberFormatException ignored) {}
				}
			}
			if (objectNode.has("integrated_gpu")) {
				JsonNode valNode = objectNode.get("integrated_gpu");
				if (valNode.isTextual()) {
					String rawVal = valNode.asText().toLowerCase().trim();
					if (rawVal.equals("có") || rawVal.equals("yes") || rawVal.equals("true")) {
						objectNode.put("integrated_gpu", true);
					} else if (rawVal.equals("không") || rawVal.equals("no") || rawVal.equals("false")) {
						objectNode.put("integrated_gpu", false);
					}
				}
			}
		} else if ("STORAGE".equalsIgnoreCase(componentType)) {
			// Normalize keys
			java.util.Map<String, String> ssdMappings = new java.util.HashMap<>();
			ssdMappings.put("lo_i_ssd", "ssd_type");
			ssdMappings.put("loại ssd", "ssd_type");
			ssdMappings.put("ssd_type", "ssd_type");

			ssdMappings.put("k_ch_c_form_factor", "form_factor");
			ssdMappings.put("kích cỡ form factor", "form_factor");
			ssdMappings.put("form_factor", "form_factor");

			ssdMappings.put("giao_di_n_k_t_n_i", "interface");
			ssdMappings.put("giao diện kết nối", "interface");
			ssdMappings.put("giao tiếp", "interface");
			ssdMappings.put("interface", "interface");

			ssdMappings.put("capacity_gb", "capacity_gb");
			ssdMappings.put("capacity", "capacity_gb");
			ssdMappings.put("dung lượng", "capacity_gb");

			ssdMappings.put("t_c_c", "read_speed_mbps");
			ssdMappings.put("tốc độ đọc", "read_speed_mbps");
			ssdMappings.put("read_speed_mbps", "read_speed_mbps");

			ssdMappings.put("t_c_ghi", "write_speed_mbps");
			ssdMappings.put("tốc độ ghi", "write_speed_mbps");
			ssdMappings.put("write_speed_mbps", "write_speed_mbps");

			ssdMappings.put("lo_i_chip_nh", "nand_type");
			ssdMappings.put("loại chip nhớ", "nand_type");
			ssdMappings.put("nand_type", "nand_type");

			ssdMappings.put("mtbf", "mtbf");
			ssdMappings.put("nhi_t_ho_t_ng", "operating_temperature");
			ssdMappings.put("nhiệt độ hoạt động", "operating_temperature");
			ssdMappings.put("operating_temperature", "operating_temperature");

			ssdMappings.put("tản nhiệt ram", "has_heatsink"); // handle the crawler mismatch
			ssdMappings.put("tản nhiệt", "has_heatsink");
			ssdMappings.put("has_heatsink", "has_heatsink");

			ssdMappings.put("tbw_b_n_ghi", "tbw");
			ssdMappings.put("tbw", "tbw");
			ssdMappings.put("cache", "cache");

			java.util.List<String> keys = new java.util.ArrayList<>();
			objectNode.fieldNames().forEachRemaining(keys::add);

			for (String key : keys) {
				String cleanKey = toSnakeCase(key);
				String standardKey = ssdMappings.get(cleanKey);
				if (standardKey != null) {
					JsonNode valueNode = objectNode.get(key);
					if (!objectNode.has(standardKey) || !key.equals(standardKey)) {
						objectNode.set(standardKey, valueNode);
					}
					if (!key.equals(standardKey)) {
						objectNode.remove(key);
					}
				}
			}

			// Value normalizations
			if (objectNode.has("capacity_gb")) {
				JsonNode capNode = objectNode.get("capacity_gb");
				if (capNode.isTextual()) {
					String rawVal = capNode.asText().toLowerCase().trim();
					if (rawVal.contains("tb") || rawVal.equals("1") || rawVal.equals("2") || rawVal.equals("4") || rawVal.equals("8")) {
						String numOnly = rawVal.replaceAll("[^0-9.]", "").trim();
						try {
							double tbVal = Double.parseDouble(numOnly);
							objectNode.put("capacity_gb", (int) (tbVal * 1000));
						} catch (NumberFormatException e) {
							objectNode.put("capacity_gb", rawVal);
						}
					} else {
						String cleanVal = rawVal.replaceAll("(?i)\\s*(gb|mb)\\b", "").trim();
						try {
							int cleanInt = Integer.parseInt(cleanVal);
							objectNode.put("capacity_gb", cleanInt);
						} catch (NumberFormatException e) {
							objectNode.put("capacity_gb", cleanVal);
						}
					}
				}
			}

			if (objectNode.has("read_speed_mbps")) {
				JsonNode valNode = objectNode.get("read_speed_mbps");
				if (valNode.isTextual()) {
					String rawVal = valNode.asText();
					String cleanVal = rawVal.toLowerCase().replaceAll("[^0-9]", "").trim();
					try {
						objectNode.put("read_speed_mbps", Integer.parseInt(cleanVal));
					} catch (NumberFormatException ignored) {}
				}
			}

			if (objectNode.has("write_speed_mbps")) {
				JsonNode valNode = objectNode.get("write_speed_mbps");
				if (valNode.isTextual()) {
					String rawVal = valNode.asText();
					String cleanVal = rawVal.toLowerCase().replaceAll("[^0-9]", "").trim();
					try {
						objectNode.put("write_speed_mbps", Integer.parseInt(cleanVal));
					} catch (NumberFormatException ignored) {}
				}
			}

			if (objectNode.has("tbw")) {
				JsonNode valNode = objectNode.get("tbw");
				if (valNode.isTextual()) {
					String rawVal = valNode.asText();
					String cleanVal = rawVal.toLowerCase().replaceAll("[^0-9]", "").trim();
					try {
						objectNode.put("tbw", Integer.parseInt(cleanVal));
					} catch (NumberFormatException ignored) {}
				}
			}

			if (objectNode.has("mtbf")) {
				JsonNode valNode = objectNode.get("mtbf");
				if (valNode.isTextual()) {
					String rawVal = valNode.asText();
					String cleanVal = rawVal.toLowerCase().replaceAll("[^0-9]", "").trim();
					try {
						objectNode.put("mtbf", Integer.parseInt(cleanVal));
					} catch (NumberFormatException ignored) {}
				}
			}

			if (objectNode.has("has_heatsink")) {
				JsonNode tnNode = objectNode.get("has_heatsink");
				if (tnNode.isTextual()) {
					String rawVal = tnNode.asText().toLowerCase().trim();
					if (rawVal.equals("có") || rawVal.equals("yes") || rawVal.equals("true") || rawVal.contains("nhiệt")) {
						objectNode.put("has_heatsink", true);
					} else if (rawVal.equals("không") || rawVal.equals("no") || rawVal.equals("false")) {
						objectNode.put("has_heatsink", false);
					}
				}
			}
		}
	}

	private String toSnakeCase(String text) {
		if (text == null) return "";
		return text.toLowerCase()
				.replaceAll("[^a-z0-9]+", "_")
				.replaceAll("^_+|_+$", "");
	}
}

package com.edu.pcmaster.common.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import java.text.Normalizer;
import java.util.*;

public class ProductSpecNormalizer {

	private static final Map<String, String> LABEL_TRANSLATIONS = new HashMap<>();

	static {
		// General translations
		LABEL_TRANSLATIONS.put("thương hiệu", "brand");
		LABEL_TRANSLATIONS.put("bảo hành", "warranty");
		LABEL_TRANSLATIONS.put("dông sản phẩm", "product_series");
		LABEL_TRANSLATIONS.put("dòng sản phẩm", "product_series");
		LABEL_TRANSLATIONS.put("dòng vga", "vga_series");
		LABEL_TRANSLATIONS.put("số nhân cuda cores (nvidia)", "cuda_cores");
		LABEL_TRANSLATIONS.put("số nhân cuda cores", "cuda_cores");
		LABEL_TRANSLATIONS.put("cuda_cores", "cuda_cores");
		LABEL_TRANSLATIONS.put("ai tops", "ai_tops");
		LABEL_TRANSLATIONS.put("xung nhịp gpu base", "base_clock");
		LABEL_TRANSLATIONS.put("xung nhịp gpu boost", "boost_clock");
		LABEL_TRANSLATIONS.put("bộ nhớ (vram)", "vram");
		LABEL_TRANSLATIONS.put("bộ nhớ vram", "vram");
		LABEL_TRANSLATIONS.put("vram", "vram");
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

		// Case mappings
		LABEL_TRANSLATIONS.put("bo mạch hỗ trợ", "supported_mainboards");
		LABEL_TRANSLATIONS.put("hỗ trợ main", "supported_mainboards");
		LABEL_TRANSLATIONS.put("h_tr_main", "supported_mainboards");
		LABEL_TRANSLATIONS.put("kích thước vỏ máy", "case_size");
		LABEL_TRANSLATIONS.put("kích thước case", "case_size");
		LABEL_TRANSLATIONS.put("k_ch_th_c_case", "case_size");
		LABEL_TRANSLATIONS.put("màu sắc", "color");
		LABEL_TRANSLATIONS.put("m_u_s_c", "color");
		LABEL_TRANSLATIONS.put("số lượng quạt đi kèm", "fan_count_included");
		LABEL_TRANSLATIONS.put("s_l_ng_qu_t_i_k_m", "fan_count_included");
		LABEL_TRANSLATIONS.put("chiều cao tản nhiệt cpu tối đa", "max_cpu_cooler_height_mm");
		LABEL_TRANSLATIONS.put("chi_u_cao_t_n_nhi_t_cpu_t_i_a", "max_cpu_cooler_height_mm");
		LABEL_TRANSLATIONS.put("chất liệu", "material");
		LABEL_TRANSLATIONS.put("ch_t_li_u", "material");
		LABEL_TRANSLATIONS.put("cổng usb 3.0", "usb_3_0_ports");
		LABEL_TRANSLATIONS.put("c_ng_usb_3_0", "usb_3_0_ports");
		LABEL_TRANSLATIONS.put("cổng usb type-c", "usb_type_c_ports");
		LABEL_TRANSLATIONS.put("cổng usb type c", "usb_type_c_ports");
		LABEL_TRANSLATIONS.put("c_ng_usb_type_c", "usb_type_c_ports");
		LABEL_TRANSLATIONS.put("hỗ trợ nguồn (psu)", "supported_psu");
		LABEL_TRANSLATIONS.put("hỗ trợ nguồn psu", "supported_psu");
		LABEL_TRANSLATIONS.put("h_tr_ngu_n_psu", "supported_psu");
		LABEL_TRANSLATIONS.put("hỗ trợ tản nhiệt nước (radiator)", "supported_radiators");
		LABEL_TRANSLATIONS.put("hỗ trợ tản nhiệt nước", "supported_radiators");
		LABEL_TRANSLATIONS.put("h_tr_t_n_nhi_t_n_c_radiator", "supported_radiators");
		LABEL_TRANSLATIONS.put("khe cắm mở rộng pci", "pci_slots");
		LABEL_TRANSLATIONS.put("khe_c_m_m_r_ng_pci", "pci_slots");
		LABEL_TRANSLATIONS.put("khoang ổ đĩa quang (odd)", "odd_bays");
		LABEL_TRANSLATIONS.put("khoang ổ đĩa quang", "odd_bays");
		LABEL_TRANSLATIONS.put("khoang_a_quang_odd", "odd_bays");
		LABEL_TRANSLATIONS.put("led rgb", "has_rgb");
		LABEL_TRANSLATIONS.put("đèn led rgb", "has_rgb");
		LABEL_TRANSLATIONS.put("led_rgb", "has_rgb");
		LABEL_TRANSLATIONS.put("mặt kính cường lực", "tempered_glass_side");
		LABEL_TRANSLATIONS.put("m_t_k_nh_c_ng_l_c", "tempered_glass_side");
		LABEL_TRANSLATIONS.put("hỗ trợ quạt mặt dưới", "bottom_fan_support");
		LABEL_TRANSLATIONS.put("quạt tản nhiệt mặt dưới", "bottom_fan_support");
		LABEL_TRANSLATIONS.put("qu_t_t_n_nhi_t_m_t_d_i", "bottom_fan_support");
		LABEL_TRANSLATIONS.put("khay gắn ổ cứng (ssd/hdd)", "drive_bays");
		LABEL_TRANSLATIONS.put("khay gắn ổ cứng", "drive_bays");
		LABEL_TRANSLATIONS.put("khay_g_n_c_ng", "drive_bays");
		LABEL_TRANSLATIONS.put("hỗ trợ quạt mặt sau", "rear_fan_support");
		LABEL_TRANSLATIONS.put("quạt tản nhiệt mặt sau", "rear_fan_support");
		LABEL_TRANSLATIONS.put("qu_t_t_n_nhi_t_m_t_sau", "rear_fan_support");
		LABEL_TRANSLATIONS.put("hỗ trợ quạt mặt trên", "top_fan_support");
		LABEL_TRANSLATIONS.put("quạt tản nhiệt mặt trên", "top_fan_support");
		LABEL_TRANSLATIONS.put("qu_t_t_n_nhi_t_m_t_tr_n", "top_fan_support");
		LABEL_TRANSLATIONS.put("hỗ trợ quạt mặt trước", "front_fan_support");
		LABEL_TRANSLATIONS.put("quạt tản nhiệt mặt trước", "front_fan_support");
		LABEL_TRANSLATIONS.put("qu_t_t_n_nhi_t_m_t_tr_c", "front_fan_support");
		LABEL_TRANSLATIONS.put("vị trí đặt nguồn", "psu_position");
		LABEL_TRANSLATIONS.put("v_tr_t_ngu_n", "psu_position");
		LABEL_TRANSLATIONS.put("d_i_vga_t_i_a", "max_gpu_length_mm");
		LABEL_TRANSLATIONS.put("chiều dài vga tối đa", "max_gpu_length_mm");
		LABEL_TRANSLATIONS.put("độ dài gpu tối đa", "max_gpu_length_mm");
		LABEL_TRANSLATIONS.put("độ dài vga tối đa", "max_gpu_length_mm");
		LABEL_TRANSLATIONS.put("c_ng_usb_2_0", "usb_2_0_ports");
		LABEL_TRANSLATIONS.put("cổng usb 2.0", "usb_2_0_ports");
		LABEL_TRANSLATIONS.put("c_ng_audio", "audio_ports");
		LABEL_TRANSLATIONS.put("cổng audio", "audio_ports");
		LABEL_TRANSLATIONS.put("cổng âm thanh", "audio_ports");

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

		// P-core and E-core
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

		// RAM mappings
		LABEL_TRANSLATIONS.put("kit_ram", "kit");
		LABEL_TRANSLATIONS.put("h_tr_ram", "kit");
		LABEL_TRANSLATIONS.put("t_c_quay_c_a_fan", "fan_speed");
		
		// GPU mappings
		LABEL_TRANSLATIONS.put("vram_gb", "vram");
		LABEL_TRANSLATIONS.put("vram", "vram");
		
		// Cooler mappings
		LABEL_TRANSLATIONS.put("lo_i_s_n_ph_m", "cooler_type");
		LABEL_TRANSLATIONS.put("loại sản phẩm", "cooler_type");
		LABEL_TRANSLATIONS.put("tản nhiệt khí", "cooler_type");
		LABEL_TRANSLATIONS.put("tản nhiệt nước", "cooler_type");
		LABEL_TRANSLATIONS.put("s_qu_t", "fan_count");
		LABEL_TRANSLATIONS.put("số quạt", "fan_count");
		LABEL_TRANSLATIONS.put("t_ng_th_ch_cpu", "cpu_socket_support");
		LABEL_TRANSLATIONS.put("tương thích cpu", "cpu_socket_support");
		LABEL_TRANSLATIONS.put("hỗ trợ socket", "cpu_socket_support");
		LABEL_TRANSLATIONS.put("ti_ng_n_pump", "pump_noise_db");
		LABEL_TRANSLATIONS.put("tiếng ồn pump", "pump_noise_db");
		LABEL_TRANSLATIONS.put("tiếng ồn bơm", "pump_noise_db");
		LABEL_TRANSLATIONS.put("k_ch_th_c_qu_t_t_n", "fan_size_mm");
		LABEL_TRANSLATIONS.put("kích thước quạt tản", "fan_size_mm");
		LABEL_TRANSLATIONS.put("kích thước quạt", "fan_size_mm");
		LABEL_TRANSLATIONS.put("t_c_qu_t", "fan_speed_rpm");
		LABEL_TRANSLATIONS.put("tốc độ quạt", "fan_speed_rpm");
		LABEL_TRANSLATIONS.put("tốc độ quay quạt", "fan_speed_rpm");
		LABEL_TRANSLATIONS.put("p_su_t_t_nh", "static_pressure_mmh2o");
		LABEL_TRANSLATIONS.put("áp suất tĩnh", "static_pressure_mmh2o");
		LABEL_TRANSLATIONS.put("lu_ng_kh", "airflow_cfm");
		LABEL_TRANSLATIONS.put("lưu lượng khí", "airflow_cfm");
		LABEL_TRANSLATIONS.put("lưu lượng gió", "airflow_cfm");
		LABEL_TRANSLATIONS.put("k_ch_th_c_pump", "pump_dimensions");
		LABEL_TRANSLATIONS.put("kích thước pump", "pump_dimensions");
		LABEL_TRANSLATIONS.put("kích thước bơm", "pump_dimensions");
		LABEL_TRANSLATIONS.put("led", "led_type");
		LABEL_TRANSLATIONS.put("đèn led", "led_type");
		LABEL_TRANSLATIONS.put("tính năng đặc biệt", "special_features");
		LABEL_TRANSLATIONS.put("t_nh_n_ng_c_bi_t", "special_features");
		LABEL_TRANSLATIONS.put("tính năng nổi bật", "special_features");
		LABEL_TRANSLATIONS.put("tu_i_th_qu_t", "fan_lifespan");
		LABEL_TRANSLATIONS.put("tuổi thọ quạt", "fan_lifespan");
		LABEL_TRANSLATIONS.put("tuổi thọ", "fan_lifespan");
		LABEL_TRANSLATIONS.put("lo_i_v_ng_bi", "bearing_type");
		LABEL_TRANSLATIONS.put("loại vòng bi", "bearing_type");
		LABEL_TRANSLATIONS.put("vòng bi", "bearing_type");
		LABEL_TRANSLATIONS.put("v_t_li_u_heat_sink", "heatsink_material");
		LABEL_TRANSLATIONS.put("vật liệu heat sink", "heatsink_material");
		LABEL_TRANSLATIONS.put("vật liệu tản nhiệt", "heatsink_material");
		LABEL_TRANSLATIONS.put("radiator_dimensions", "radiator_dimensions");
		LABEL_TRANSLATIONS.put("kích thước radiator", "radiator_dimensions");
		LABEL_TRANSLATIONS.put("kích thước radiator tản nhiệt", "radiator_dimensions");
		LABEL_TRANSLATIONS.put("chi_u_d_i_ng", "tube_length");
		LABEL_TRANSLATIONS.put("chiều dài ống", "tube_length");
		LABEL_TRANSLATIONS.put("chiều dài ống dẫn", "tube_length");
		LABEL_TRANSLATIONS.put("độ dài ống", "tube_length");

		// Storage mappings
		LABEL_TRANSLATIONS.put("ssd_type", "ssd_type");
		LABEL_TRANSLATIONS.put("loại ssd", "ssd_type");
		LABEL_TRANSLATIONS.put("lo_i_ssd", "ssd_type");
		LABEL_TRANSLATIONS.put("tốc độ đọc", "read_speed_mbps");
		LABEL_TRANSLATIONS.put("t_c_c_mb_s", "read_speed_mbps");
		LABEL_TRANSLATIONS.put("tốc độ ghi", "write_speed_mbps");
		LABEL_TRANSLATIONS.put("t_c_ghi_mb_s", "write_speed_mbps");
		LABEL_TRANSLATIONS.put("tbw_w", "tbw");
		LABEL_TRANSLATIONS.put("tbw", "tbw");
		LABEL_TRANSLATIONS.put("tbw độ bền ghi", "tbw");
		LABEL_TRANSLATIONS.put("tbw_b_n_ghi", "tbw");
		LABEL_TRANSLATIONS.put("độ bền ghi", "tbw");
		LABEL_TRANSLATIONS.put("o_b_n_ghi", "tbw");
		LABEL_TRANSLATIONS.put("has_heatsink", "has_heatsink");
		LABEL_TRANSLATIONS.put("tản nhiệt", "has_heatsink");
		LABEL_TRANSLATIONS.put("t_n_nhi_t", "has_heatsink");
		LABEL_TRANSLATIONS.put("k_ch_c_form_factor", "form_factor");
		LABEL_TRANSLATIONS.put("kích thước form factor", "form_factor");
		LABEL_TRANSLATIONS.put("giao diện kết nối", "interface");
		LABEL_TRANSLATIONS.put("giao_di_n_k_t_n_i", "interface");
		LABEL_TRANSLATIONS.put("lo_i_chip_nh", "nand_type");
		LABEL_TRANSLATIONS.put("loại chip nhớ", "nand_type");
		LABEL_TRANSLATIONS.put("mtbf", "mtbf");
		LABEL_TRANSLATIONS.put("nhi_t_ho_t_ng", "operating_temperature");
		LABEL_TRANSLATIONS.put("nhiệt độ hoạt động", "operating_temperature");
		LABEL_TRANSLATIONS.put("bộ nhớ đệm", "cache");
		LABEL_TRANSLATIONS.put("b_nh_m", "cache");
		LABEL_TRANSLATIONS.put("cache", "cache");

		// Fan mappings
		LABEL_TRANSLATIONS.put("lo_i_k_t_n_i", "connection_type");
		LABEL_TRANSLATIONS.put("loại kết nối", "connection_type");
		LABEL_TRANSLATIONS.put("chuẩn kết nối", "connection_type");
		LABEL_TRANSLATIONS.put("chuẩn cắm", "connection_type");
		LABEL_TRANSLATIONS.put("lo_i_qu_t", "fan_type");
		LABEL_TRANSLATIONS.put("loại quạt", "fan_type");
		LABEL_TRANSLATIONS.put("lo_i_n_led", "led_type");
		LABEL_TRANSLATIONS.put("loại đèn led", "led_type");
		LABEL_TRANSLATIONS.put("lo_i_tr_c", "bearing_type");
		LABEL_TRANSLATIONS.put("loại trục", "bearing_type");
		LABEL_TRANSLATIONS.put("loại trục quay", "bearing_type");
		LABEL_TRANSLATIONS.put("lưu lượng gió (cfm)", "airflow_cfm");
		LABEL_TRANSLATIONS.put("lưu lượng gió", "airflow_cfm");
		LABEL_TRANSLATIONS.put("lưu lượng khí", "airflow_cfm");
		LABEL_TRANSLATIONS.put("lu_ng_kh", "airflow_cfm");
		LABEL_TRANSLATIONS.put("tuổi thọ quạt", "fan_lifespan");
		LABEL_TRANSLATIONS.put("tu_i_th_qu_t", "fan_lifespan");
		LABEL_TRANSLATIONS.put("t_c_quay", "fan_speed_rpm");
		LABEL_TRANSLATIONS.put("tốc độ quay", "fan_speed_rpm");
		LABEL_TRANSLATIONS.put("áp suất tĩnh (mmh₂o)", "static_pressure_mmh2o");
		LABEL_TRANSLATIONS.put("áp suất tĩnh (mmh2o)", "static_pressure_mmh2o");
		LABEL_TRANSLATIONS.put("áp suất tĩnh", "static_pressure_mmh2o");
		LABEL_TRANSLATIONS.put("p_su_t_t_nh", "static_pressure_mmh2o");
		LABEL_TRANSLATIONS.put("điện áp", "voltage");
		LABEL_TRANSLATIONS.put("tiếng ồn", "noise_level_db");
		LABEL_TRANSLATIONS.put("n", "noise_level_db");
		LABEL_TRANSLATIONS.put("độ ồn", "noise_level_db");
		LABEL_TRANSLATIONS.put("độ ồn (db)", "noise_level_db");
		LABEL_TRANSLATIONS.put("kích thước quạt (mm)", "size_mm");
		LABEL_TRANSLATIONS.put("kích thước quạt", "size_mm");

		// Laptop mappings
		LABEL_TRANSLATIONS.put("cpu", "cpu");
		LABEL_TRANSLATIONS.put("ram", "ram");
		LABEL_TRANSLATIONS.put("s_khe_ram", "ram_slots");
		LABEL_TRANSLATIONS.put("khe cắm ram", "ram_slots");
		LABEL_TRANSLATIONS.put("khe ram", "ram_slots");
		LABEL_TRANSLATIONS.put("ssd", "ssd");
		LABEL_TRANSLATIONS.put("s_khe_ssd", "ssd_slots");
		LABEL_TRANSLATIONS.put("khe cắm ssd", "ssd_slots");
		LABEL_TRANSLATIONS.put("khe ssd", "ssd_slots");
		LABEL_TRANSLATIONS.put("chu_n_ssd", "ssd_type");
		LABEL_TRANSLATIONS.put("chuẩn ssd", "ssd_type");
		LABEL_TRANSLATIONS.put("k_ch_th_c_m_n_h_nh", "screen_size");
		LABEL_TRANSLATIONS.put("kích thước màn hình", "screen_size");
		LABEL_TRANSLATIONS.put("t_n_s_qu_t", "refresh_rate");
		LABEL_TRANSLATIONS.put("tần số quét", "refresh_rate");
		LABEL_TRANSLATIONS.put("tần số quét màn hình", "refresh_rate");
		LABEL_TRANSLATIONS.put("c_ng_ngh_m_n_h_nh", "screen_tech");
		LABEL_TRANSLATIONS.put("công nghệ màn hình", "screen_tech");
		LABEL_TRANSLATIONS.put("chất liệu vỏ / mặt trước", "material");
		LABEL_TRANSLATIONS.put("chất liệu vỏ", "material");
		LABEL_TRANSLATIONS.put("ch_t_li_u_v_m_n_h_nh", "material");
		LABEL_TRANSLATIONS.put("chu_n_wifi_bluetooth", "connectivity");
		LABEL_TRANSLATIONS.put("chuẩn wifi bluetooth", "connectivity");
		LABEL_TRANSLATIONS.put("h_i_u_h_nh", "os");
		LABEL_TRANSLATIONS.put("hệ điều hành", "os");
		LABEL_TRANSLATIONS.put("webcam", "webcam");
		LABEL_TRANSLATIONS.put("pin", "battery");
		LABEL_TRANSLATIONS.put("adapter", "battery");
		LABEL_TRANSLATIONS.put("nhu_c_u_s_d_ng", "intended_use");
		LABEL_TRANSLATIONS.put("nhu cầu sử dụng", "intended_use");
		LABEL_TRANSLATIONS.put("k_ch_th_c_m_y", "dimensions");
		LABEL_TRANSLATIONS.put("kích thước máy", "dimensions");
		LABEL_TRANSLATIONS.put("s_ng_m_n_h_nh", "brightness_cdm2");
		LABEL_TRANSLATIONS.put("độ sáng màn hình", "brightness_cdm2");
		LABEL_TRANSLATIONS.put("độ sáng", "brightness_cdm2");
		LABEL_TRANSLATIONS.put("c_ng_ngh_m_thanh", "audio_tech");
		LABEL_TRANSLATIONS.put("công nghệ âm thanh", "audio_tech");
		LABEL_TRANSLATIONS.put("m_n_h_nh_c_m_ng", "touchscreen");
		LABEL_TRANSLATIONS.put("màn hình cảm ứng", "touchscreen");
		LABEL_TRANSLATIONS.put("b_n_ph_m_c_n", "has_numpad");
		LABEL_TRANSLATIONS.put("bàn phím số", "has_numpad");
		LABEL_TRANSLATIONS.put("bàn phím có số", "has_numpad");
		LABEL_TRANSLATIONS.put("nhu_c_u_s_d_ng_laptop", "intended_use");
		LABEL_TRANSLATIONS.put("nhu cầu sử dụng laptop", "intended_use");
		LABEL_TRANSLATIONS.put("laptop_2_trong_1", "is_two_in_one");
		LABEL_TRANSLATIONS.put("laptop 2 trong 1", "is_two_in_one");
		LABEL_TRANSLATIONS.put("card_h_a", "vga");
		LABEL_TRANSLATIONS.put("card đồ họa", "vga");
		LABEL_TRANSLATIONS.put("t_nh_ch_t_b_m_t", "screen_finish");
		LABEL_TRANSLATIONS.put("tính chất bề mặt", "screen_finish");
		LABEL_TRANSLATIONS.put("bề mặt màn hình", "screen_finish");
		LABEL_TRANSLATIONS.put("ph_n_gi_i", "resolution");
		LABEL_TRANSLATIONS.put("độ phân giải", "resolution");
		LABEL_TRANSLATIONS.put("chu_n_m_u", "color_accuracy");
		LABEL_TRANSLATIONS.put("chuẩn màu", "color_accuracy");
		LABEL_TRANSLATIONS.put("độ chuẩn màu", "color_accuracy");
	}

	public static String translateKey(String key, String componentType) {
		if (key == null) return "";
		String keyLower = key.toLowerCase().trim();

		// Custom translation mapping for PSU to map "form factor" to "dimensions"
		if ("PSU".equalsIgnoreCase(componentType)) {
			String snake = toSnakeCase(keyLower);
			if (snake.equals("form_factor") || snake.equals("form_factor_psu")) {
				return "dimensions";
			}
		}

		// Custom translation mapping for FAN to map size keys to "size_mm"
		if ("FAN".equalsIgnoreCase(componentType)) {
			String snake = toSnakeCase(keyLower);
			if (snake.equals("fan_size_mm") || snake.equals("k_ch_th_c_qu_t_t_n") || snake.equals("k_ch_th_c_qu_t") || snake.equals("kich_thuoc_quat") || snake.equals("kich_thuoc_quat_mm") || snake.equals("k_ch_th_c_qu_t_mm") || keyLower.contains("kích thước quạt")) {
				return "size_mm";
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
	public static String normalizeValueString(String key, String val) {
		if (val == null) return "";
		String valTrimmed = cleanString(val);
		String valLower = valTrimmed.toLowerCase();

		// Normalize boolean values
		if (valLower.equals("có") || valLower.equals("yes") || valLower.equals("true")) {
			return "true";
		}
		if (valLower.equals("không") || valLower.equals("no") || valLower.equals("false")) {
			return "false";
		}

		// Broad check for known boolean keys to resolve values starting with "Có"/"Không"
		if (key.startsWith("has_") || key.startsWith("is_") || key.contains("wifi") || key.contains("rgb") || key.equals("water_cooled") || key.equals("tempered_glass_side") || key.equals("touchscreen") || key.equals("has_numpad")) {
			if (valLower.startsWith("không") || valLower.equals("no") || valLower.equals("false") || valLower.isEmpty()) {
				return "false";
			}
			if (valLower.startsWith("có") || valLower.equals("yes") || valLower.equals("true") || valLower.contains("tản nhiệt nhôm")) {
				return "true";
			}
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
			valTrimmed = valTrimmed.replaceAll("(?i)\\s*(cores|threads| nhân| luồng|khe|cổng| slots| slot| ram| quạt| quat| cái| cai)", "");
		}
		if ((key.endsWith("_mm") || key.contains("height") || key.contains("length")) && !key.contains("radiator")) {
			valTrimmed = valTrimmed.replaceAll("(?i)\\s*(mm| mm)", "");
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

	public static String toSnakeCase(String text) {
		if (text == null) return "";
		return text.toLowerCase()
				.replaceAll("[^a-z0-9]+", "_")
				.replaceAll("^_+|_+$", "");
	}

	public static String cleanString(String text) {
		if (text == null) return "";
		String normalized = Normalizer.normalize(text, Normalizer.Form.NFC);
		normalized = normalized.replaceAll("[\\s\\h\\xa0\\u2007\\u202F\\u00A0]+", " ");
		return normalized.trim();
	}

	public static JsonNode normalize(JsonNode specsJson, String componentType) {
		if (specsJson == null || !specsJson.isObject()) {
			return specsJson == null ? JsonNodeFactory.instance.objectNode() : specsJson;
		}

		ObjectNode objectNode = (ObjectNode) specsJson;
		List<String> keys = new ArrayList<>();
		objectNode.fieldNames().forEachRemaining(keys::add);

		// 1. Rename keys using the central map
		for (String key : keys) {
			String standardKey = translateKey(key, componentType);
			if (standardKey != null && !standardKey.isEmpty()) {
				JsonNode valueNode = objectNode.get(key);
				if (!objectNode.has(standardKey) || !key.equals(standardKey)) {
					objectNode.set(standardKey, valueNode);
				}
				if (!key.equals(standardKey)) {
					objectNode.remove(key);
				}
			}
		}

		// 2. Perform value type / format normalization based on the standardized key and component type
		// PSU
		if ("PSU".equalsIgnoreCase(componentType)) {
			if (objectNode.has("wattage")) {
				JsonNode wattageNode = objectNode.get("wattage");
				if (wattageNode.isTextual()) {
					String cleanVal = normalizeValueString("wattage", wattageNode.asText());
					try {
						objectNode.put("wattage", Integer.parseInt(cleanVal));
					} catch (NumberFormatException e) {
						objectNode.put("wattage", cleanVal);
					}
				}
			}
			if (objectNode.has("efficiency_percent")) {
				JsonNode effNode = objectNode.get("efficiency_percent");
				if (effNode.isTextual()) {
					String cleanVal = effNode.asText().replaceAll("[^0-9.]", "").trim();
					try {
						objectNode.put("efficiency_percent", Double.parseDouble(cleanVal));
					} catch (NumberFormatException e) {
						objectNode.put("efficiency_percent", cleanVal);
					}
				}
			}
			if (objectNode.has("fan_size_mm")) {
				JsonNode fsNode = objectNode.get("fan_size_mm");
				if (fsNode.isTextual()) {
					String cleanVal = fsNode.asText().replaceAll("[^0-9]", "").trim();
					try {
						objectNode.put("fan_size_mm", Integer.parseInt(cleanVal));
					} catch (NumberFormatException e) {
						objectNode.put("fan_size_mm", cleanVal);
					}
				}
			}
		}
		// RAM
		else if ("RAM".equalsIgnoreCase(componentType)) {
			if (objectNode.has("capacity_gb")) {
				JsonNode capNode = objectNode.get("capacity_gb");
				if (capNode.isTextual()) {
					String cleanVal = normalizeValueString("capacity_gb", capNode.asText());
					try {
						objectNode.put("capacity_gb", Integer.parseInt(cleanVal));
					} catch (NumberFormatException e) {
						objectNode.put("capacity_gb", cleanVal);
					}
				}
			}
			if (objectNode.has("bus_speed_mhz")) {
				JsonNode busNode = objectNode.get("bus_speed_mhz");
				if (busNode.isTextual()) {
					String cleanVal = busNode.asText().replaceAll("[^0-9]", "").trim();
					try {
						objectNode.put("bus_speed_mhz", Integer.parseInt(cleanVal));
					} catch (NumberFormatException e) {
						objectNode.put("bus_speed_mhz", cleanVal);
					}
				}
			}
			if (objectNode.has("module_count")) {
				JsonNode mcNode = objectNode.get("module_count");
				if (mcNode.isTextual()) {
					String cleanVal = mcNode.asText().replaceAll("(?i)\\s*(thanh|modules|module|pcs)\\b", "").trim();
					try {
						objectNode.put("module_count", Integer.parseInt(cleanVal));
					} catch (NumberFormatException e) {
						objectNode.put("module_count", cleanVal);
					}
				}
			}
			if (objectNode.has("latency_cl")) {
				JsonNode latNode = objectNode.get("latency_cl");
				if (latNode.isTextual()) {
					String cleanVal = normalizeValueString("latency_cl", latNode.asText());
					try {
						objectNode.put("latency_cl", Integer.parseInt(cleanVal));
					} catch (NumberFormatException e) {
						objectNode.put("latency_cl", cleanVal);
					}
				}
			}
			if (objectNode.has("has_rgb")) {
				JsonNode rgbNode = objectNode.get("has_rgb");
				if (rgbNode.isTextual()) {
					objectNode.put("has_rgb", Boolean.parseBoolean(normalizeValueString("has_rgb", rgbNode.asText())));
				}
			}
		}
		// GPU
		else if ("GPU".equalsIgnoreCase(componentType)) {
			if (objectNode.has("vram")) {
				JsonNode vramNode = objectNode.get("vram");
				if (vramNode.isTextual()) {
					String cleanVal = normalizeValueString("capacity_gb", vramNode.asText());
					try {
						objectNode.put("vram", Integer.parseInt(cleanVal));
					} catch (NumberFormatException e) {
						objectNode.put("vram", cleanVal);
					}
				}
			}
			if (objectNode.has("recommended_psu")) {
				JsonNode psuNode = objectNode.get("recommended_psu");
				if (psuNode.isTextual()) {
					String cleanVal = normalizeValueString("recommended_psu", psuNode.asText());
					try {
						objectNode.put("recommended_psu", Integer.parseInt(cleanVal));
					} catch (NumberFormatException e) {
						objectNode.put("recommended_psu", cleanVal);
					}
				}
			}
			if (objectNode.has("min_psu_w")) {
				JsonNode psuNode = objectNode.get("min_psu_w");
				if (psuNode.isTextual()) {
					String cleanVal = normalizeValueString("wattage", psuNode.asText());
					try {
						objectNode.put("min_psu_w", Integer.parseInt(cleanVal));
					} catch (NumberFormatException e) {
						objectNode.put("min_psu_w", cleanVal);
					}
				}
			}
			if (objectNode.has("tdp_w")) {
				JsonNode tdpNode = objectNode.get("tdp_w");
				if (tdpNode.isTextual()) {
					String cleanVal = normalizeValueString("tdp_w", tdpNode.asText());
					try {
						objectNode.put("tdp_w", Integer.parseInt(cleanVal));
					} catch (NumberFormatException e) {
						objectNode.put("tdp_w", cleanVal);
					}
				}
			}
			if (objectNode.has("length_mm")) {
				JsonNode lenNode = objectNode.get("length_mm");
				if (lenNode.isTextual()) {
					String cleanVal = normalizeValueString("length_mm", lenNode.asText());
					try {
						objectNode.put("length_mm", Integer.parseInt(cleanVal));
					} catch (NumberFormatException e) {
						objectNode.put("length_mm", cleanVal);
					}
				}
			}
		}
		// CPU
		else if ("CPU".equalsIgnoreCase(componentType)) {
			if (objectNode.has("cores")) {
				JsonNode coresNode = objectNode.get("cores");
				if (coresNode.isTextual()) {
					String cleanVal = normalizeValueString("cores", coresNode.asText());
					try {
						objectNode.put("cores", Integer.parseInt(cleanVal));
					} catch (NumberFormatException e) {
						objectNode.put("cores", cleanVal);
					}
				}
			}
			if (objectNode.has("threads")) {
				JsonNode thNode = objectNode.get("threads");
				if (thNode.isTextual()) {
					String cleanVal = normalizeValueString("threads", thNode.asText());
					try {
						objectNode.put("threads", Integer.parseInt(cleanVal));
					} catch (NumberFormatException e) {
						objectNode.put("threads", cleanVal);
					}
				}
			}
			if (objectNode.has("tdp_w")) {
				JsonNode tdpNode = objectNode.get("tdp_w");
				if (tdpNode.isTextual()) {
					String cleanVal = normalizeValueString("tdp_w", tdpNode.asText());
					try {
						objectNode.put("tdp_w", Integer.parseInt(cleanVal));
					} catch (NumberFormatException e) {
						objectNode.put("tdp_w", cleanVal);
					}
				}
			}
			if (objectNode.has("integrated_gpu")) {
				JsonNode igpuNode = objectNode.get("integrated_gpu");
				if (igpuNode.isTextual()) {
					objectNode.put("integrated_gpu", Boolean.parseBoolean(normalizeValueString("integrated_gpu", igpuNode.asText())));
				}
			}
		}
		// STORAGE
		else if ("STORAGE".equalsIgnoreCase(componentType)) {
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
						String cleanVal = normalizeValueString("capacity_gb", rawVal);
						try {
							objectNode.put("capacity_gb", Integer.parseInt(cleanVal));
						} catch (NumberFormatException e) {
							objectNode.put("capacity_gb", rawVal);
						}
					}
				}
			}
			if (objectNode.has("read_speed_mbps")) {
				JsonNode rdNode = objectNode.get("read_speed_mbps");
				if (rdNode.isTextual()) {
					String cleanVal = rdNode.asText().replaceAll("[^0-9]", "").trim();
					try {
						objectNode.put("read_speed_mbps", Integer.parseInt(cleanVal));
					} catch (NumberFormatException e) {
						objectNode.put("read_speed_mbps", cleanVal);
					}
				}
			}
			if (objectNode.has("write_speed_mbps")) {
				JsonNode wrNode = objectNode.get("write_speed_mbps");
				if (wrNode.isTextual()) {
					String cleanVal = wrNode.asText().replaceAll("[^0-9]", "").trim();
					try {
						objectNode.put("write_speed_mbps", Integer.parseInt(cleanVal));
					} catch (NumberFormatException e) {
						objectNode.put("write_speed_mbps", cleanVal);
					}
				}
			}
			if (objectNode.has("tbw")) {
				JsonNode tbwNode = objectNode.get("tbw");
				if (tbwNode.isTextual()) {
					String cleanVal = normalizeValueString("tbw", tbwNode.asText());
					try {
						objectNode.put("tbw", Integer.parseInt(cleanVal));
					} catch (NumberFormatException e) {
						objectNode.put("tbw", cleanVal);
					}
				}
			}
			if (objectNode.has("has_heatsink")) {
				JsonNode hsNode = objectNode.get("has_heatsink");
				if (hsNode.isTextual()) {
					objectNode.put("has_heatsink", Boolean.parseBoolean(normalizeValueString("has_heatsink", hsNode.asText())));
				}
			}
			if (objectNode.has("mtbf")) {
				JsonNode mtbfNode = objectNode.get("mtbf");
				if (mtbfNode.isTextual()) {
					String cleanVal = mtbfNode.asText().replaceAll("(?i)\\s*(giờ|gio|hours|hrs)\\s*$", "").replaceAll("[^0-9]", "").trim();
					try {
						objectNode.put("mtbf", Integer.parseInt(cleanVal));
					} catch (NumberFormatException ignored) {}
				}
			}
		}
		// MAINBOARD
		else if ("MAINBOARD".equalsIgnoreCase(componentType)) {
			if (objectNode.has("max_ram_gb")) {
				JsonNode mrNode = objectNode.get("max_ram_gb");
				if (mrNode.isTextual()) {
					String cleanVal = normalizeValueString("max_ram_gb", mrNode.asText());
					try {
						objectNode.put("max_ram_gb", Integer.parseInt(cleanVal));
					} catch (NumberFormatException e) {
						objectNode.put("max_ram_gb", cleanVal);
					}
				}
			}
			if (objectNode.has("ram_slots")) {
				JsonNode rsNode = objectNode.get("ram_slots");
				if (rsNode.isTextual()) {
					String cleanVal = normalizeValueString("ram_slots", rsNode.asText());
					try {
						objectNode.put("ram_slots", Integer.parseInt(cleanVal));
					} catch (NumberFormatException e) {
						objectNode.put("ram_slots", cleanVal);
					}
				}
			}
			if (objectNode.has("m2_slots")) {
				JsonNode m2Node = objectNode.get("m2_slots");
				if (m2Node.isTextual()) {
					String cleanVal = normalizeValueString("m2_slots", m2Node.asText());
					try {
						objectNode.put("m2_slots", Integer.parseInt(cleanVal));
					} catch (NumberFormatException e) {
						objectNode.put("m2_slots", cleanVal);
					}
				}
			}
			if (objectNode.has("has_wifi")) {
				JsonNode wfNode = objectNode.get("has_wifi");
				if (wfNode.isTextual()) {
					objectNode.put("has_wifi", Boolean.parseBoolean(normalizeValueString("has_wifi", wfNode.asText())));
				}
			}
			if (objectNode.has("has_rgb")) {
				JsonNode rgbNode = objectNode.get("has_rgb");
				if (rgbNode.isTextual()) {
					objectNode.put("has_rgb", Boolean.parseBoolean(normalizeValueString("has_rgb", rgbNode.asText())));
				}
			}
		}
		// CASE
		else if ("CASE".equalsIgnoreCase(componentType)) {
			if (objectNode.has("fan_count_included")) {
				JsonNode valNode = objectNode.get("fan_count_included");
				if (valNode.isTextual()) {
					String cleanVal = normalizeValueString("fan_count_included", valNode.asText());
					try {
						objectNode.put("fan_count_included", Integer.parseInt(cleanVal));
					} catch (NumberFormatException ignored) {}
				}
			}
			if (objectNode.has("max_cpu_cooler_height_mm")) {
				JsonNode valNode = objectNode.get("max_cpu_cooler_height_mm");
				if (valNode.isTextual()) {
					String cleanVal = normalizeValueString("max_cpu_cooler_height_mm", valNode.asText());
					try {
						objectNode.put("max_cpu_cooler_height_mm", Integer.parseInt(cleanVal));
					} catch (NumberFormatException ignored) {}
				}
			}
			if (objectNode.has("usb_3_0_ports")) {
				JsonNode valNode = objectNode.get("usb_3_0_ports");
				if (valNode.isTextual()) {
					String cleanVal = normalizeValueString("usb_3_0_ports", valNode.asText());
					try {
						objectNode.put("usb_3_0_ports", Integer.parseInt(cleanVal));
					} catch (NumberFormatException ignored) {}
				}
			}
			if (objectNode.has("usb_type_c_ports")) {
				JsonNode valNode = objectNode.get("usb_type_c_ports");
				if (valNode.isTextual()) {
					String cleanVal = normalizeValueString("usb_type_c_ports", valNode.asText());
					try {
						objectNode.put("usb_type_c_ports", Integer.parseInt(cleanVal));
					} catch (NumberFormatException ignored) {}
				}
			}
			if (objectNode.has("max_gpu_length_mm")) {
				JsonNode valNode = objectNode.get("max_gpu_length_mm");
				if (valNode.isTextual()) {
					String cleanVal = normalizeValueString("max_gpu_length_mm", valNode.asText());
					try {
						objectNode.put("max_gpu_length_mm", Integer.parseInt(cleanVal));
					} catch (NumberFormatException ignored) {}
				}
			}
			if (objectNode.has("usb_2_0_ports")) {
				JsonNode valNode = objectNode.get("usb_2_0_ports");
				if (valNode.isTextual()) {
					String cleanVal = normalizeValueString("usb_2_0_ports", valNode.asText());
					try {
						objectNode.put("usb_2_0_ports", Integer.parseInt(cleanVal));
					} catch (NumberFormatException ignored) {}
				}
			}
			if (objectNode.has("has_rgb")) {
				JsonNode valNode = objectNode.get("has_rgb");
				if (valNode.isTextual()) {
					objectNode.put("has_rgb", Boolean.parseBoolean(normalizeValueString("has_rgb", valNode.asText())));
				}
			}
			if (objectNode.has("tempered_glass_side")) {
				JsonNode valNode = objectNode.get("tempered_glass_side");
				if (valNode.isTextual()) {
					objectNode.put("tempered_glass_side", Boolean.parseBoolean(normalizeValueString("tempered_glass_side", valNode.asText())));
				}
			}
			if (objectNode.has("pci_slots")) {
				JsonNode valNode = objectNode.get("pci_slots");
				if (valNode.isTextual()) {
					String cleanVal = normalizeValueString("pci_slots", valNode.asText());
					try {
						objectNode.put("pci_slots", Integer.parseInt(cleanVal));
					} catch (NumberFormatException ignored) {}
				}
			}
			if (objectNode.has("drive_bays")) {
				JsonNode valNode = objectNode.get("drive_bays");
				if (valNode.isTextual()) {
					String cleanVal = normalizeValueString("drive_bays", valNode.asText());
					try {
						objectNode.put("drive_bays", Integer.parseInt(cleanVal));
					} catch (NumberFormatException ignored) {}
				}
			}
			if (objectNode.has("odd_bays")) {
				JsonNode valNode = objectNode.get("odd_bays");
				if (valNode.isTextual()) {
					objectNode.put("odd_bays", Boolean.parseBoolean(normalizeValueString("odd_bays", valNode.asText())));
				}
			}
			if (objectNode.has("supported_mainboards") && objectNode.get("supported_mainboards").isTextual()) {
				String text = objectNode.get("supported_mainboards").asText();
				String[] parts = text.split("[,\\s]+");
				ArrayNode arrayNode = JsonNodeFactory.instance.arrayNode();
				for (String part : parts) {
					if (!part.trim().isEmpty()) {
						arrayNode.add(part.trim());
					}
				}
				objectNode.set("supported_mainboards", arrayNode);
			}
		}
		// COOLER
		else if ("COOLER".equalsIgnoreCase(componentType)) {
			if (objectNode.has("fan_count")) {
				JsonNode valNode = objectNode.get("fan_count");
				if (valNode.isTextual()) {
					String cleanVal = normalizeValueString("fan_count", valNode.asText()).replaceAll("[^0-9]", "").trim();
					try {
						objectNode.put("fan_count", Integer.parseInt(cleanVal));
					} catch (NumberFormatException ignored) {}
				}
			}
			if (objectNode.has("fan_size_mm")) {
				JsonNode valNode = objectNode.get("fan_size_mm");
				if (valNode.isTextual()) {
					String cleanVal = valNode.asText().replaceAll("[^0-9]", "").trim();
					try {
						objectNode.put("fan_size_mm", Integer.parseInt(cleanVal));
					} catch (NumberFormatException ignored) {}
				}
			}
			// fan_speed_rpm: keep as text (e.g. "650-1750 RPM ± 10%") - just strip unit suffix
			if (objectNode.has("fan_speed_rpm")) {
				JsonNode valNode = objectNode.get("fan_speed_rpm");
				if (valNode.isTextual()) {
					// Remove trailing " RPM" or " rpm" suffixes but keep range info
					String cleanVal = valNode.asText().replaceAll("(?i)\\s*RPM\\s*$", "").trim();
					objectNode.put("fan_speed_rpm", cleanVal);
				}
			}
			// pump_noise_db: keep as text (e.g. "27.2 dBA (Max)") - strip double unit
			if (objectNode.has("pump_noise_db")) {
				JsonNode valNode = objectNode.get("pump_noise_db");
				if (valNode.isTextual()) {
					String cleanVal = valNode.asText().replaceAll("(?i)\\s*dB\\s*$", "").trim();
					objectNode.put("pump_noise_db", cleanVal);
				}
			}
			// cpu_socket_support: space-delimited string → JSON array
			if (objectNode.has("cpu_socket_support") && objectNode.get("cpu_socket_support").isTextual()) {
				String text = objectNode.get("cpu_socket_support").asText();
				String[] parts = text.trim().split("[\\s,]+");
				ArrayNode arrayNode = JsonNodeFactory.instance.arrayNode();
				for (String part : parts) {
					if (!part.trim().isEmpty()) {
						arrayNode.add(part.trim());
					}
				}
				objectNode.set("cpu_socket_support", arrayNode);
			}
		}
		// FAN
		else if ("FAN".equalsIgnoreCase(componentType)) {
			if (objectNode.has("size_mm")) {
				JsonNode valNode = objectNode.get("size_mm");
				if (valNode.isTextual()) {
					String valStr = valNode.asText();
					int inferredSize = -1;
					if (valStr.contains("120")) inferredSize = 120;
					else if (valStr.contains("140")) inferredSize = 140;
					else if (valStr.contains("200")) inferredSize = 200;
					else if (valStr.contains("80")) inferredSize = 80;
					else if (valStr.contains("92")) inferredSize = 92;

					if (inferredSize != -1) {
						objectNode.put("size_mm", inferredSize);
					} else {
						String numOnly = valStr.replaceAll("[^0-9]", "").trim();
						try {
							objectNode.put("size_mm", Integer.parseInt(numOnly));
						} catch (NumberFormatException ignored) {}
					}
				}
			}
			if (objectNode.has("fan_speed_rpm")) {
				JsonNode valNode = objectNode.get("fan_speed_rpm");
				if (valNode.isTextual()) {
					String cleanVal = valNode.asText().replaceAll("(?i)\\s*RPM\\s*$", "").trim();
					try {
						objectNode.put("fan_speed_rpm", Integer.parseInt(cleanVal));
					} catch (NumberFormatException e) {
						objectNode.put("fan_speed_rpm", cleanVal);
					}
				}
			}
			if (objectNode.has("noise_level_db")) {
				JsonNode valNode = objectNode.get("noise_level_db");
				if (valNode.isTextual()) {
					String cleanVal = valNode.asText().replaceAll("(?i)\\s*dB\\s*$", "").trim();
					try {
						objectNode.put("noise_level_db", Double.parseDouble(cleanVal));
					} catch (NumberFormatException e) {
						try {
							objectNode.put("noise_level_db", Integer.parseInt(cleanVal));
						} catch (NumberFormatException e2) {
							objectNode.put("noise_level_db", cleanVal);
						}
					}
				}
			}
			if (objectNode.has("airflow_cfm")) {
				JsonNode valNode = objectNode.get("airflow_cfm");
				if (valNode.isTextual()) {
					String cleanVal = valNode.asText().replaceAll("(?i)\\s*CFM\\s*$", "").trim();
					try {
						objectNode.put("airflow_cfm", Double.parseDouble(cleanVal));
					} catch (NumberFormatException e) {
						try {
							objectNode.put("airflow_cfm", Integer.parseInt(cleanVal));
						} catch (NumberFormatException e2) {
							objectNode.put("airflow_cfm", cleanVal);
						}
					}
				}
			}
			if (objectNode.has("led_type")) {
				String led = objectNode.get("led_type").asText().toLowerCase();
				if (led.contains("argb") || led.contains("addressable")) {
					objectNode.put("has_rgb", true);
					objectNode.put("is_addressable_rgb", true);
				} else if (led.contains("rgb")) {
					objectNode.put("has_rgb", true);
					objectNode.put("is_addressable_rgb", false);
				} else if (led.contains("không") || led.contains("none") || led.contains("no")) {
					objectNode.put("has_rgb", false);
					objectNode.put("is_addressable_rgb", false);
				}
			}
		}
		// LAPTOP
		else if ("LAPTOP".equalsIgnoreCase(componentType)) {
			if (objectNode.has("ram_slots")) {
				JsonNode valNode = objectNode.get("ram_slots");
				if (valNode.isTextual()) {
					String cleanVal = normalizeValueString("ram_slots", valNode.asText()).replaceAll("[^0-9]", "").trim();
					try {
						objectNode.put("ram_slots", Integer.parseInt(cleanVal));
					} catch (NumberFormatException ignored) {}
				}
			}
			if (objectNode.has("ssd_slots")) {
				JsonNode valNode = objectNode.get("ssd_slots");
				if (valNode.isTextual()) {
					String cleanVal = normalizeValueString("ssd_slots", valNode.asText()).replaceAll("[^0-9]", "").trim();
					try {
						objectNode.put("ssd_slots", Integer.parseInt(cleanVal));
					} catch (NumberFormatException ignored) {}
				}
			}
			if (objectNode.has("weight")) {
				JsonNode valNode = objectNode.get("weight");
				if (valNode.isTextual()) {
					String cleanVal = valNode.asText().replaceAll("(?i)\\s*(kg|g| kg| g)\\s*$", "").trim();
					try {
						objectNode.put("weight", Double.parseDouble(cleanVal));
					} catch (NumberFormatException e) {
						objectNode.put("weight", cleanVal);
					}
				}
			}
			if (objectNode.has("screen_size")) {
				JsonNode valNode = objectNode.get("screen_size");
				if (valNode.isTextual()) {
					String cleanVal = valNode.asText().replaceAll("(?i)\\s*(inch|\\\"| inch)\\s*$", "").trim();
					try {
						objectNode.put("screen_size", Double.parseDouble(cleanVal));
					} catch (NumberFormatException e) {
						objectNode.put("screen_size", cleanVal);
					}
				}
			}
			if (objectNode.has("refresh_rate")) {
				JsonNode valNode = objectNode.get("refresh_rate");
				if (valNode.isTextual()) {
					String cleanVal = valNode.asText().replaceAll("(?i)\\s*(hz| hz)\\s*$", "").trim();
					try {
						objectNode.put("refresh_rate", Integer.parseInt(cleanVal));
					} catch (NumberFormatException e) {
						objectNode.put("refresh_rate", cleanVal);
					}
				}
			}
			if (objectNode.has("touchscreen")) {
				JsonNode valNode = objectNode.get("touchscreen");
				if (valNode.isTextual()) {
					objectNode.put("touchscreen", Boolean.parseBoolean(normalizeValueString("touchscreen", valNode.asText())));
				}
			}
			if (objectNode.has("has_numpad")) {
				JsonNode valNode = objectNode.get("has_numpad");
				if (valNode.isTextual()) {
					objectNode.put("has_numpad", Boolean.parseBoolean(normalizeValueString("has_numpad", valNode.asText())));
				}
			}
			if (objectNode.has("is_two_in_one")) {
				JsonNode valNode = objectNode.get("is_two_in_one");
				if (valNode.isTextual()) {
					objectNode.put("is_two_in_one", Boolean.parseBoolean(normalizeValueString("is_two_in_one", valNode.asText())));
				}
			}
		}

		// General normalization for brightness_cdm2 (Monitor or Laptop)
		if (objectNode.has("brightness_cdm2")) {
			JsonNode valNode = objectNode.get("brightness_cdm2");
			if (valNode.isTextual()) {
				String cleanVal = valNode.asText().replaceAll("(?i)\\s*(nits|nit|cd/m²|cd/m2| cd/m2| cd/m²)\\s*$", "").trim();
				try {
					objectNode.put("brightness_cdm2", Integer.parseInt(cleanVal));
				} catch (NumberFormatException e) {
					try {
						objectNode.put("brightness_cdm2", Double.parseDouble(cleanVal));
					} catch (NumberFormatException e2) {
						objectNode.put("brightness_cdm2", cleanVal);
					}
				}
			}
		}

		// General Fallback for Booleans in any properties
		for (String standardKey : keys) {
			if (objectNode.has(standardKey)) {
				JsonNode nodeVal = objectNode.get(standardKey);
				if (nodeVal.isTextual()) {
					String valText = nodeVal.asText().toLowerCase().trim();
					if (valText.equals("true") || valText.equals("false")) {
						objectNode.put(standardKey, Boolean.parseBoolean(valText));
					}
				}
			}
		}

		return objectNode;
	}
}

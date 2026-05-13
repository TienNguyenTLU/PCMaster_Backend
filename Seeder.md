Cloudinary Package structure

```
PCMAster_Storage
├── Product_thumbnails
├──----VGA
├──----CPU
├──----RAM
├──----Mainboard
├──----PSU
├──----Case
├──----SSD
├──----Fan
├──----Cooler
├──----Monitor
├── Brands_Logos
├──----VGA
├──----CPU
├──----RAM
├──----Mainboard
├──----PSU
├──----Case
├──----SSD
├──----Fan
├──----Cooler
├──----Monitor
```

Cấu trúc specs khi seed database:
CPU: 
"specs": {
"brand": "string",          // Intel, AMD
"series": "string",         // Core i9, Ryzen 7
"socket": "string",         // LGA1700, AM5, AM4
"cores": "number",          // 8, 16, 24
"threads": "number",        // 16, 32
"base_clock_ghz": "number", // 3.2
"boost_clock_ghz": "number",// 5.2
"cache_mb": "number",       // 36
"tdp_w": "number",          // 125 (Công suất tỏa nhiệt)
"integrated_gpu": "boolean",// true/false
"performance_score": "number" // Điểm benchmark để tính nghẽn (e.g. 35000)
}
VGA:
"specs": {
"brand": "string",          // NVIDIA, AMD, ASUS, MSI
"chipset": "string",        // RTX 4070, RX 7800 XT
"vram_gb": "number",        // 8, 12, 16, 24
"vram_type": "string",      // GDDR6, GDDR6X
"base_clock_mhz": "number", // 1920
"boost_clock_mhz": "number",// 2500
"tdp_w": "number",          // 285 (Điện năng tiêu thụ)
"length_mm": "number",      // 320 (Để check kích thước case)
"min_psu_w": "number",      // 750 (Công suất nguồn khuyến nghị)
"performance_score": "number" // Điểm benchmark (e.g. 28000)
}
RAM:
"specs":
{
"brand": "string",          // Corsair, G.Skill, Kingston
"type": "string",           // DDR4, DDR5
"capacity_gb": "number",    // 8, 16, 32, 64
"bus_speed_mhz": "number",  // 3200, 5600, 6000
"kit": "string",            // 1x8GB, 2x8GB, 2x16GB
"latency_cl": "number",     // 16, 30, 36
"has_rgb": "boolean"        // true/false
}

Mainboard: 
"specs": {
"brand": "string",          // ASUS, Gigabyte, ASRock
"chipset": "string",        // Z790, B660, X670, B550
"socket": "string",         // LGA1700, AM5
"form_factor": "string",    // ATX, Micro-ATX, Mini-ITX
"ram_slots": "number",      // 2, 4
"ram_type": "string",       // DDR4, DDR5
"max_ram_gb": "number",     // 128
"m2_slots": "number",       // 2, 3
"has_wifi": "boolean"       // true/false
}
SSD: 
"specs": {
"brand": "string",          // Samsung, WD, Crucial
"type": "string",           // SSD, HDD
"interface": "string",      // NVMe PCIe Gen4, SATA III
"capacity_gb": "number",    // 500, 1000, 2000
"read_speed_mbps": "number",// 7000
"write_speed_mbps": "number"// 5000
}
PSU: 
"specs": {
"brand": "string",          // Seasonic, Cooler Master
"wattage": "number",        // 650, 750, 850, 1000
"efficiency_rating": "string", // 80 Plus Gold, Platinum
"modularity": "string",     // Full Modular, Semi-Modular, Non-Modular
"form_factor": "string"     // ATX, SFX
}
Case: 
"specs": {
"brand": "string",
"size": "string",           // Mid Tower, Full Tower, ITX
"supported_mainboards": ["string"], // ["ATX", "M-ATX"]
"max_gpu_length_mm": "number", // 350
"max_cpu_cooler_height_mm": "number" // 165
}
Monitor: 
"specs": {
"brand": "string",            // Dell, LG, ASUS, Samsung
"size_inch": "number",        // 24, 27, 32
"resolution": "string",       // 1920x1080, 2560x1440, 3840x2160
"panel_type": "string",       // IPS, VA, TN, OLED
"refresh_rate_hz": "number",  // 60, 144, 165, 240
"response_time_ms": "number", // 1, 4, 5
"brightness_cdm2": "number",  // 250, 350, 400
"aspect_ratio": "string",     // 16:9, 21:9 (Ultrawide)
"color_accuracy": "string",   // 99% sRGB, 95% DCI-P3
"has_hdr": "boolean",         // true/false
"ports": ["string"]           // ["HDMI 2.1", "DisplayPort 1.4"]
}
Cooler:
"specs":{
"brand": "string",            // Noctua, Corsair, Deepcool
"type": "string",             // Air Cooling (Khí), Liquid Cooling (Nước)
"supported_sockets": ["string"], // ["LGA1700", "AM5", "AM4"]
"tdp_rating_w": "number",     // 150, 250 (Khả năng tản nhiệt tối đa)
"fan_size_mm": "number",      // 120, 140
"height_mm": "number",        // 158 (Dùng để check với thông số Case)
"radiator_size_mm": "number", // 240, 280, 360 (Nếu là tản nước)
"has_rgb": "boolean",         // true/false
"noise_level_db": "number"    // 25, 30
}
Fan:
"specs":{
"brand": "string",            // Cooler Master, Lian Li, Arctic
"size_mm": "number",          // 120, 140
"fan_speed_rpm": "number",    // 1500, 2000
"airflow_cfm": "number",      // 50.5, 65.0 (Lưu lượng gió)
"noise_level_db": "number",   // 22, 28
"bearing_type": "string",     // Hydraulic, Dual Ball
"connection_type": "string",  // 4-pin PWM, 3-pin
"has_rgb": "boolean",         // true/false
"is_addressable_rgb": "boolean" // true (ARGB) / false (RGB thường)
}
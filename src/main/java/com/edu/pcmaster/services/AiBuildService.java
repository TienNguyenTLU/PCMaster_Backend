package com.edu.pcmaster.services;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Service;

import com.edu.pcmaster.dto.ai.CpuAdviceResponse;
import com.edu.pcmaster.dto.ai.PsuRecommendationResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class AiBuildService {
	private final ChatModel chatModel;
	private final ObjectMapper objectMapper = new ObjectMapper();

	public AiBuildService(ChatModel chatModel) {
		this.chatModel = chatModel;
	}

	public PsuRecommendationResponse getPsuRecommendation(String cpu, String gpu, String ram) {
		String cpuName = cpu != null ? cpu.trim() : "";
		String gpuName = gpu != null ? gpu.trim() : "";
		String ramName = ram != null ? ram.trim() : "";

		String promptText = String.format("""
				Bạn là chuyên gia tư vấn nguồn máy tính (PSU). Hãy tính toán công suất nguồn khuyên dùng dựa trên các linh kiện sau:
				- CPU: %s
				- GPU: %s
				- RAM: %s

				Yêu cầu:
				1. Hãy tính toán tổng công suất tiêu thụ tối đa ước tính (TDP).
				2. Đề xuất công suất nguồn (PSU) khuyến nghị (ví dụ: 650W, 750W, 850W...).
				3. Trả về kết quả dưới định dạng JSON với cấu trúc chính xác sau:
				{
				  "wattage": <số nguyên công suất khuyến nghị, ví dụ: 750>,
				  "explanation": "<giải thích ngắn gọn bằng tiếng Việt khoảng 2-3 câu về lý do chọn công suất này>"
				}
				Không viết thêm bất kỳ dòng text nào khác ngoài chuỗi JSON này.
				""", cpuName, gpuName, ramName);

		try {
			String response = chatModel.call(new Prompt(promptText)).getResult().getOutput().getText();
			String jsonText = extractJson(response);
			JsonNode root = objectMapper.readTree(jsonText);
			int wattage = root.path("wattage").asInt(0);
			String explanation = root.path("explanation").asText("");

			if (wattage > 0 && !explanation.isEmpty()) {
				return new PsuRecommendationResponse(wattage, explanation);
			}
		} catch (Exception e) {
			System.err.println("[AI Build] Failed to call Ollama for PSU recommendation, falling back to static calculation: " + e.getMessage());
		}

		// Fallback to static calculations
		return getFallbackPsuRecommendation(cpuName, gpuName);
	}

	public CpuAdviceResponse getCpuAdvice(String cpuName) {
		String name = cpuName != null ? cpuName.trim() : "";
		String promptText = String.format("""
				Bạn là chuyên gia phần cứng máy tính. Hãy đưa ra gợi ý chipset bo mạch chủ (Mainboard) khuyên dùng cho CPU sau:
				%s

				Yêu cầu:
				1. Phân tích phân khúc của CPU (ví dụ: cao cấp, tầm trung, phổ thông).
				2. Khuyên dùng dòng chipset bo mạch chủ tối ưu (Ví dụ: Ryzen 7 9950X3D hoặc Ryzen 9 9950X3D khuyên dùng mainboard chipset dòng X trở lên như X670/X870 để có VRM tốt nhất).
				3. Trả về kết quả dưới định dạng JSON với cấu trúc chính xác sau:
				{
				  "advice": "<lời khuyên ngắn gọn bằng tiếng Việt, khoảng 2-3 câu>"
				}
				Không viết thêm bất kỳ dòng text nào khác ngoài chuỗi JSON này.
				""", name);

		try {
			String response = chatModel.call(new Prompt(promptText)).getResult().getOutput().getText();
			String jsonText = extractJson(response);
			JsonNode root = objectMapper.readTree(jsonText);
			String advice = root.path("advice").asText("");

			if (!advice.isEmpty()) {
				return new CpuAdviceResponse(name, advice);
			}
		} catch (Exception e) {
			System.err.println("[AI Build] Failed to call Ollama for CPU advice, falling back to static rules: " + e.getMessage());
		}

		// Fallback to static rules
		return getFallbackCpuAdvice(name);
	}

	private String extractJson(String text) {
		if (text == null) return null;
		text = text.trim();
		int start = text.indexOf('{');
		int end = text.lastIndexOf('}');
		if (start != -1 && end != -1 && end > start) {
			return text.substring(start, end + 1);
		}
		return text;
	}

	private PsuRecommendationResponse getFallbackPsuRecommendation(String cpuName, String gpuName) {
		int cpuTdp = 100; // default fallback
		int gpuTdp = 200; // default fallback

		String cpuLower = cpuName.toLowerCase();
		if (cpuLower.contains("i9") || cpuLower.contains("ryzen 9") || cpuLower.contains("9950") || cpuLower.contains("14900") || cpuLower.contains("13900")) {
			cpuTdp = 250;
		} else if (cpuLower.contains("i7") || cpuLower.contains("ryzen 7") || cpuLower.contains("9700") || cpuLower.contains("7800") || cpuLower.contains("14700") || cpuLower.contains("13700")) {
			cpuTdp = 150;
		} else if (cpuLower.contains("i5") || cpuLower.contains("ryzen 5") || cpuLower.contains("9600") || cpuLower.contains("7600") || cpuLower.contains("14600") || cpuLower.contains("13600")) {
			cpuTdp = 100;
		}

		String gpuLower = gpuName.toLowerCase();
		if (gpuLower.contains("4090") || gpuLower.contains("3090")) {
			gpuTdp = 450;
		} else if (gpuLower.contains("4080") || gpuLower.contains("3080") || gpuLower.contains("7900")) {
			gpuTdp = 320;
		} else if (gpuLower.contains("4070") || gpuLower.contains("3070") || gpuLower.contains("7800")) {
			gpuTdp = 220;
		} else if (gpuLower.contains("4060") || gpuLower.contains("3060") || gpuLower.contains("7700") || gpuLower.contains("7600")) {
			gpuTdp = 160;
		}

		int totalTdp = cpuTdp + gpuTdp;
		int recommendedWattage = (int) Math.ceil((totalTdp + 150) / 50.0) * 50;
		if (recommendedWattage < 500) recommendedWattage = 500; // safe minimum

		String explanation = String.format("Đề xuất công suất nguồn tối thiểu %dW dựa trên công suất tỏa nhiệt (TDP) ước tính của CPU (%dW) và GPU (%dW) kèm hệ số an toàn 150W. (Tính toán tự động ngoại tuyến)",
				recommendedWattage, cpuTdp, gpuTdp);

		return new PsuRecommendationResponse(recommendedWattage, explanation);
	}

	private CpuAdviceResponse getFallbackCpuAdvice(String cpuName) {
		String cpuLower = cpuName.toLowerCase();
		String advice;

		if (cpuLower.contains("i9") || cpuLower.contains("ryzen 9") || cpuLower.contains("9950") || cpuLower.contains("9900") || cpuLower.contains("7950") || cpuLower.contains("7900") || cpuLower.contains("14900") || cpuLower.contains("13900")) {
			advice = String.format("Vi xử lý [%s] thuộc phân khúc cao cấp hiệu năng cực cao. Bạn nên ưu tiên sử dụng các dòng bo mạch chủ chipset cao cấp (như Z790/Z890 của Intel hoặc X670/X870 của AMD) sở hữu hệ thống VRM nhiều pha cấp nguồn chất lượng để tránh bị nghẽn hiệu năng.", cpuName);
		} else if (cpuLower.contains("i7") || cpuLower.contains("ryzen 7") || cpuLower.contains("9700") || cpuLower.contains("7800") || cpuLower.contains("7700") || cpuLower.contains("14700") || cpuLower.contains("13700")) {
			advice = String.format("Vi xử lý [%s] thuộc phân khúc cận cao cấp mạnh mẽ. Bạn nên lựa chọn các bo mạch chủ chipset tầm trung hoặc cao cấp như B760/Z790 (Intel) hoặc B650/X670 (AMD) để khai thác tối đa sức mạnh mà vẫn cân đối được chi phí.", cpuName);
		} else {
			advice = String.format("Vi xử lý [%s] thuộc phân khúc phổ thông. Bạn có thể sử dụng các bo mạch chủ dòng phổ thông hoặc tầm trung như H610/B760 (Intel) hoặc A620/B650 (AMD) để tối ưu hóa chi phí lắp ráp.", cpuName);
		}

		return new CpuAdviceResponse(cpuName, advice);
	}
}

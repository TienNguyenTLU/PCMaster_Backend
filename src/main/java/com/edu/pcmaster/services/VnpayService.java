package com.edu.pcmaster.services;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.stereotype.Service;

import com.edu.pcmaster.config.VnpayConfig;
import com.edu.pcmaster.models.Order;

@Service
public class VnpayService {
	private final VnpayConfig vnpayConfig;

	public VnpayService(VnpayConfig vnpayConfig) {
		this.vnpayConfig = vnpayConfig;
	}

	public String generatePaymentUrl(Order order, String ipAddress) {
		String vnp_Version = "2.1.0";
		String vnp_Command = "pay";
		String vnp_TxnRef = order.getId().toString();
		String vnp_OrderInfo = "Thanh toan don hang #" + order.getId();
		String vnp_OrderType = "other";
		String vnp_Locale = "vn";

		long amount = order.getTotalAmount().multiply(new java.math.BigDecimal(100)).longValue();

		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss")
				.withZone(ZoneId.of("Asia/Ho_Chi_Minh"));
		String vnp_CreateDate = formatter.format(Instant.now());

		Map<String, String> vnp_Params = new HashMap<>();
		vnp_Params.put("vnp_Version", vnp_Version);
		vnp_Params.put("vnp_Command", vnp_Command);
		vnp_Params.put("vnp_TmnCode", vnpayConfig.getTmnCode());
		vnp_Params.put("vnp_Amount", String.valueOf(amount));
		vnp_Params.put("vnp_CurrCode", "VND");
		vnp_Params.put("vnp_TxnRef", vnp_TxnRef);
		vnp_Params.put("vnp_OrderInfo", vnp_OrderInfo);
		vnp_Params.put("vnp_OrderType", vnp_OrderType);
		vnp_Params.put("vnp_Locale", vnp_Locale);
		vnp_Params.put("vnp_ReturnUrl", vnpayConfig.getReturnUrl());
		
		String ip = ipAddress;
		if (ip == null || ip.isBlank() || ip.equals("0:0:0:0:0:0:0:1") || ip.equals("::1") || ip.contains(":")) {
			ip = "127.0.0.1";
		}
		vnp_Params.put("vnp_IpAddr", ip);
		vnp_Params.put("vnp_CreateDate", vnp_CreateDate);

		return buildUrl(vnp_Params);
	}

	private String buildUrl(Map<String, String> params) {
		List<String> fieldNames = new ArrayList<>(params.keySet());
		Collections.sort(fieldNames);
		List<String> hashPairs = new ArrayList<>();
		List<String> queryPairs = new ArrayList<>();
		for (String fieldName : fieldNames) {
			String fieldValue = params.get(fieldName);
			if (fieldValue != null && !fieldValue.isEmpty()) {
				try {
					String encodedValue = URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII.toString());
					String encodedName = URLEncoder.encode(fieldName, StandardCharsets.US_ASCII.toString());
					hashPairs.add(fieldName + "=" + encodedValue);
					queryPairs.add(encodedName + "=" + encodedValue);
				} catch (UnsupportedEncodingException e) {
					// ignore
				}
			}
		}
		String hashData = String.join("&", hashPairs);
		String query = String.join("&", queryPairs);
		String secureHash = hmacSHA512(vnpayConfig.getSecretKey(), hashData);
		return vnpayConfig.getPayUrl() + "?" + query + "&vnp_SecureHash=" + secureHash;
	}

	public boolean verifyCallback(Map<String, String> fields) {
		String vnp_SecureHash = fields.get("vnp_SecureHash");
		if (vnp_SecureHash == null) {
			return false;
		}

		// Remove non-vnp parameters and hash fields from signature calculation
		Map<String, String> signFields = new HashMap<>();
		for (Map.Entry<String, String> entry : fields.entrySet()) {
			String key = entry.getKey();
			if (key.startsWith("vnp_") && !key.equals("vnp_SecureHash") && !key.equals("vnp_SecureHashType")) {
				signFields.put(key, entry.getValue());
			}
		}

		List<String> fieldNames = new ArrayList<>(signFields.keySet());
		Collections.sort(fieldNames);
		List<String> kvPairs = new ArrayList<>();
		for (String fieldName : fieldNames) {
			String fieldValue = signFields.get(fieldName);
			if (fieldValue != null && !fieldValue.isEmpty()) {
				try {
					String encodedValue = URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII.toString());
					kvPairs.add(fieldName + "=" + encodedValue);
				} catch (UnsupportedEncodingException e) {
					// ignore
				}
			}
		}

		String data = String.join("&", kvPairs);
		String calculatedHash = hmacSHA512(vnpayConfig.getSecretKey(), data);
		return calculatedHash.equalsIgnoreCase(vnp_SecureHash);
	}

	public static String hmacSHA512(final String key, final String data) {
		try {
			if (key == null || data == null) {
				return "";
			}
			final Mac hmac512 = Mac.getInstance("HmacSHA512");
			byte[] hmacKeyBytes = key.getBytes(StandardCharsets.UTF_8);
			final SecretKeySpec secretKey = new SecretKeySpec(hmacKeyBytes, "HmacSHA512");
			hmac512.init(secretKey);
			byte[] dataBytes = data.getBytes(StandardCharsets.UTF_8);
			byte[] result = hmac512.doFinal(dataBytes);
			StringBuilder sb = new StringBuilder(2 * result.length);
			for (byte b : result) {
				sb.append(String.format("%02x", b & 0xff));
			}
			return sb.toString();
		} catch (Exception ex) {
			return "";
		}
	}
}

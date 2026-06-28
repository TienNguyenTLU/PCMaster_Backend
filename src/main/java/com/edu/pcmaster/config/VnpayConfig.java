package com.edu.pcmaster.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import lombok.Getter;

@Configuration
@Getter
public class VnpayConfig {
	@Value("${vnp.payUrl}")
	private String payUrl;

	@Value("${vnp.tmnCode}")
	private String tmnCode;

	@Value("${vnp.secretKey}")
	private String secretKey;

	@Value("${vnp.returnUrl}")
	private String returnUrl;

	@Value("${vnp.apiUrl}")
	private String apiUrl;
}

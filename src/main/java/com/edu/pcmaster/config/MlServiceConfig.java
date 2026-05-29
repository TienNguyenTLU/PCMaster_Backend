package com.edu.pcmaster.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class MlServiceConfig {

	@Value("${pcmaster.ml.base-url:http://localhost:8000}")
	private String mlBaseUrl;

	@Value("${pcmaster.ml.timeout-ms:5000}")
	private int timeoutMs;

	@Bean
	public RestClient mlRestClient() {
		return RestClient.builder()
				.baseUrl(mlBaseUrl)
				.build();
	}
}

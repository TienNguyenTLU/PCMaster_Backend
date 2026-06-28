package com.edu.pcmaster;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootApplication
public class PcmasterApplication {

	public static void main(String[] args) {
		SpringApplication.run(PcmasterApplication.class, args);
	}

}

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

	@Bean
	public CommandLineRunner dropCheckConstraint(JdbcTemplate jdbcTemplate) {
		return args -> {
			try {
				jdbcTemplate.execute("ALTER TABLE users DROP CONSTRAINT IF EXISTS users_role_check");
				System.out.println("PCMASTER MIGRATION: Successfully dropped users_role_check constraint.");
			} catch (Exception e) {
				System.err.println("PCMASTER MIGRATION: Failed to drop users_role_check constraint: " + e.getMessage());
			}
		};
	}
}

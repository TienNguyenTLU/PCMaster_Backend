package com.edu.pcmaster.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class DatabaseSchemaFixer implements CommandLineRunner {

	private final JdbcTemplate jdbcTemplate;

	public DatabaseSchemaFixer(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	@Override
	public void run(String... args) throws Exception {
		System.out.println("[DatabaseSchemaFixer] Checking and updating database schema for Order delivery fields...");
		try {
			// Add delivery_type if missing (PostgreSQL/H2 friendly syntax)
			jdbcTemplate.execute("ALTER TABLE orders ADD COLUMN IF NOT EXISTS delivery_type VARCHAR(30) DEFAULT 'SHOWROOM_PICKUP'");
			
			// Ensure it has default and set not-null
			jdbcTemplate.execute("ALTER TABLE orders ALTER COLUMN delivery_type SET DEFAULT 'SHOWROOM_PICKUP'");
			jdbcTemplate.execute("UPDATE orders SET delivery_type = 'SHOWROOM_PICKUP' WHERE delivery_type IS NULL");
			
			// Add other nullable delivery/document columns if missing
			jdbcTemplate.execute("ALTER TABLE orders ADD COLUMN IF NOT EXISTS recipient_name VARCHAR(150)");
			jdbcTemplate.execute("ALTER TABLE orders ADD COLUMN IF NOT EXISTS recipient_phone VARCHAR(30)");
			jdbcTemplate.execute("ALTER TABLE orders ADD COLUMN IF NOT EXISTS shipping_address VARCHAR(500)");
			jdbcTemplate.execute("ALTER TABLE orders ADD COLUMN IF NOT EXISTS document_url VARCHAR(255)");

			// Add phone and address to users if missing
			jdbcTemplate.execute("ALTER TABLE users ADD COLUMN IF NOT EXISTS phone VARCHAR(30)");
			jdbcTemplate.execute("ALTER TABLE users ADD COLUMN IF NOT EXISTS address VARCHAR(500)");

			System.out.println("[DatabaseSchemaFixer] Database schema for Order and User has been successfully verified/updated!");
		} catch (Exception e) {
			System.err.println("[DatabaseSchemaFixer] Error updating database schema: " + e.getMessage());
		}
	}
}

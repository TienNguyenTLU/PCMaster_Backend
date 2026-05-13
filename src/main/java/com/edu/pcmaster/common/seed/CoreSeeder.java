package com.edu.pcmaster.common.seed;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.edu.pcmaster.models.Brand;
import com.edu.pcmaster.models.Supplier;
import com.edu.pcmaster.models.User;
import com.edu.pcmaster.models.UserRole;
import com.edu.pcmaster.repositories.BrandRepository;
import com.edu.pcmaster.repositories.SupplierRepository;
import com.edu.pcmaster.repositories.UserRepository;

@Component
@ConditionalOnProperty(prefix = "app.seed.core", name = "enabled", havingValue = "true")
public class CoreSeeder implements CommandLineRunner {
	private static final Logger log = LoggerFactory.getLogger(CoreSeeder.class);

	private final UserRepository userRepository;
	private final SupplierRepository supplierRepository;
	private final BrandRepository brandRepository;
	private final PasswordEncoder passwordEncoder;

	public CoreSeeder(UserRepository userRepository,
					 SupplierRepository supplierRepository,
					 BrandRepository brandRepository,
					 PasswordEncoder passwordEncoder) {
		this.userRepository = userRepository;
		this.supplierRepository = supplierRepository;
		this.brandRepository = brandRepository;
		this.passwordEncoder = passwordEncoder;
	}

	@Override
	@Transactional
	public void run(String... args) {
		seedUsers();
		seedSuppliers();
	}

	private void seedUsers() {
		createUserIfMissing("admin", "admin@pcmaster.local", "Admin@123", UserRole.ADMIN);
		createUserIfMissing("customer", "customer@pcmaster.local", "Customer@123", UserRole.CUSTOMER);
		createUserIfMissing("buyer", "buyer@pcmaster.local", "Buyer@123", UserRole.CUSTOMER);
	}

	private void createUserIfMissing(String username, String email, String rawPassword, UserRole role) {
		if (userRepository.existsByUsername(username) || userRepository.existsByEmail(email)) {
			return;
		}
		User user = new User();
		user.setUsername(username);
		user.setEmail(email);
		user.setPasswordHash(passwordEncoder.encode(rawPassword));
		user.setRole(role);
		userRepository.save(user);
		log.info("Seeded user {}", username);
	}

	private void seedSuppliers() {
		List<Brand> brands = ensureBrands();
		if (brands.isEmpty()) {
			log.warn("No brands available for supplier seeding");
			return;
		}

		List<SupplierSeed> suppliers = List.of(
				new SupplierSeed("An Phat Trading", "contact@anphat.local", "0901000001", "Hanoi", "Nguyen An"),
				new SupplierSeed("Hoang Ha Tech", "sales@hoangha.local", "0901000002", "HCM", "Tran Ha"),
				new SupplierSeed("Phong Vu Distribution", "hello@phongvu.local", "0901000003", "Da Nang", "Le Vu"),
				new SupplierSeed("Synnex FPT", "partner@synnex.local", "0901000004", "Hanoi", "Pham Linh"),
				new SupplierSeed("GearVN Supply", "supply@gearvn.local", "0901000005", "HCM", "Do Quang")
		);

		for (SupplierSeed seed : suppliers) {
			Supplier supplier = supplierRepository.findByNameIgnoreCase(seed.name())
					.orElseGet(() -> new Supplier());
			supplier.setName(seed.name());
			supplier.setEmail(seed.email());
			supplier.setPhone(seed.phone());
			supplier.setAddress(seed.address());
			supplier.setContactPerson(seed.contactPerson());
			supplier.setBrands(pickBrands(brands));
			supplierRepository.save(supplier);
		}
	}

	private List<Brand> ensureBrands() {
		List<Brand> brands = brandRepository.findAll();
		if (!brands.isEmpty()) {
			return brands;
		}

		List<String> fallback = List.of(
				"Asus", "Gigabyte", "MSI", "Asrock", "Intel",
				"AMD", "Corsair", "Kingston", "Samsung", "Cooler Master",
				"Razer", "NZXT", "Seagate", "Western Digital", "Thermaltake"
		);
		List<Brand> created = new ArrayList<>();
		for (String name : fallback) {
			Brand brand = brandRepository.findByNameIgnoreCase(name)
					.orElseGet(() -> {
						Brand newBrand = new Brand();
						newBrand.setName(name);
						return brandRepository.save(newBrand);
					});
			created.add(brand);
		}
		return created;
	}

	private Set<Brand> pickBrands(List<Brand> allBrands) {
		List<Brand> copy = new ArrayList<>(allBrands);
		Collections.shuffle(copy, ThreadLocalRandom.current());
		int min = Math.min(5, copy.size());
		int max = Math.min(10, copy.size());
		int count = ThreadLocalRandom.current().nextInt(min, max + 1);
		return new LinkedHashSet<>(copy.subList(0, count));
	}

	private record SupplierSeed(String name, String email, String phone, String address, String contactPerson) {
	}
}


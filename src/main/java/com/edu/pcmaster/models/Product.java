package com.edu.pcmaster.models;

import java.math.BigDecimal;
import java.time.Instant;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "products")
@jakarta.persistence.EntityListeners(ProductListener.class)
@Getter
@Setter
public class Product {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "category_id")
	private Category category;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "brand_id")
	private Brand brand;

	@Column(nullable = false, length = 255)
	private String name;

	@Column(nullable = false, unique = true, length = 255)
	private String slug;

	@Column(nullable = false, precision = 12, scale = 2)
	private BigDecimal price;

	@Column(nullable = false)
	private Integer stock = 0;

	@Column(name = "thumbnail_url")
	private String thumbnailUrl;

	@Column(columnDefinition = "text")
	private String description;

	@Column(name = "specs", columnDefinition = "jsonb")
	@JdbcTypeCode(SqlTypes.JSON)
	private JsonNode specsJson;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	@OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
	@com.fasterxml.jackson.annotation.JsonIgnore
	private List<PcSystemComponent> pcComponents = new ArrayList<>();

	@OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
	@com.fasterxml.jackson.annotation.JsonIgnore
	private List<ProductImage> images = new ArrayList<>();

	@ManyToMany(mappedBy = "products", fetch = FetchType.LAZY)
	@com.fasterxml.jackson.annotation.JsonIgnore
	private List<Promotion> promotions = new ArrayList<>();

	@PrePersist
	void onCreate() {
		Instant now = Instant.now();
		createdAt = now;
		updatedAt = now;
		normalizeSpecs();
	}

	@PreUpdate
	void onUpdate() {
		updatedAt = Instant.now();
		normalizeSpecs();
	}

	
	private void normalizeSpecs() {
		if (specsJson == null || !specsJson.isObject()) {
			specsJson = JsonNodeFactory.instance.objectNode();
			return;
		}

		
		String componentType = "";
		if (specsJson.has("component_type")) {
			componentType = specsJson.get("component_type").asText();
		} else if (category != null && category.getSlug() != null) {
			String slug = category.getSlug().toLowerCase().replace("-", "");
			if (slug.contains("psu") || slug.contains("power") || slug.contains("nguon")) {
				componentType = "PSU";
			} else if (slug.contains("ram") || slug.contains("memory")) {
				componentType = "RAM";
			} else if (slug.contains("mainboard") || slug.contains("mother") || slug.contains("board")) {
				componentType = "MAINBOARD";
			} else if (slug.contains("vga") || slug.contains("gpu") || slug.contains("graphic")) {
				componentType = "GPU";
			} else if (slug.contains("cpu") || slug.contains("processor") || slug.contains("vi-xu-ly")) {
				componentType = "CPU";
			} else if (slug.contains("ssd") || slug.contains("hdd") || slug.contains("storage") || slug.contains("o-cung")) {
				componentType = "STORAGE";
			} else if (slug.contains("case") || slug.contains("vomay") || slug.contains("vomaytinh")) {
				componentType = "CASE";
			}
		}

		specsJson = com.edu.pcmaster.common.util.ProductSpecNormalizer.normalize(specsJson, componentType);
	}
}

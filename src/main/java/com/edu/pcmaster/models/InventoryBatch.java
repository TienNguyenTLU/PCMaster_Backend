package com.edu.pcmaster.models;

import java.math.BigDecimal;
import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "inventory_batches")
@Getter
@Setter
public class InventoryBatch {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "product_id")
	private Product product;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "purchase_order_item_id")
	private PurchaseOrderItem purchaseOrderItem;

	@Column(nullable = false)
	private Integer quantity;

	@Column(name = "remaining_quantity", nullable = false)
	private Integer remainingQuantity;

	@Column(name = "import_price", precision = 12, scale = 2)
	private BigDecimal importPrice;

	@Column(name = "imported_at", nullable = false)
	private Instant importedAt;

	@PrePersist
	void onCreate() {
		importedAt = Instant.now();
	}
}


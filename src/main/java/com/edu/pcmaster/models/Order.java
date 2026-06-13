package com.edu.pcmaster.models;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "orders")
@Getter
@Setter
public class Order {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id")
	private User user;

	@Column(name = "total_amount", precision = 12, scale = 2)
	private BigDecimal totalAmount;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private OrderStatus status = OrderStatus.DRAFT;

	// ── Delivery ────────────────────────────────────────────────────────────

	@Enumerated(EnumType.STRING)
	@Column(name = "delivery_type", nullable = false, length = 30)
	private DeliveryType deliveryType = DeliveryType.SHOWROOM_PICKUP;

	/** Full name of recipient (required for HOME_DELIVERY) */
	@Column(name = "recipient_name", length = 150)
	private String recipientName;

	/** Phone number of recipient */
	@Column(name = "recipient_phone", length = 30)
	private String recipientPhone;

	/** Delivery address (required for HOME_DELIVERY) */
	@Column(name = "shipping_address", length = 500)
	private String shippingAddress;

	/** Cloudinary URL of the XLSX export document, set when order is confirmed */
	@Column(name = "document_url")
	private String documentUrl;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<OrderItem> items = new ArrayList<>();

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "coupon_id")
	private Coupon coupon;

	@Column(name = "coupon_discount", precision = 12, scale = 2)
	private BigDecimal couponDiscount = BigDecimal.ZERO;

	@PrePersist
	void onCreate() {
		createdAt = Instant.now();
	}
}

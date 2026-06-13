package com.edu.pcmaster.models;

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
@Table(name = "inventory_issue_slips")
@Getter
@Setter
public class InventoryIssueSlip {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true, length = 30)
	private String code;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "order_id", nullable = true)
	private Order order;

	@Column(name = "export_reason", length = 100)
	private String exportReason;

	@jakarta.persistence.OneToMany(mappedBy = "slip", cascade = jakarta.persistence.CascadeType.ALL, orphanRemoval = true)
	private java.util.List<InventoryIssueSlipItem> items = new java.util.ArrayList<>();

	@Column(nullable = false, length = 20)
	private String status = "PENDING"; // PENDING, COMPLETED

	@Column(name = "document_url")
	private String documentUrl;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@Column(name = "completed_at")
	private Instant completedAt;

	@PrePersist
	void onCreate() {
		createdAt = Instant.now();
	}
}

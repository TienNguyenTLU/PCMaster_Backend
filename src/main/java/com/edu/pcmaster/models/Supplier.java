package com.edu.pcmaster.models;

import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "suppliers")
@Getter
@Setter
public class Supplier {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 200)
	private String name;

	@Column(length = 100)
	private String email;

	@Column(length = 20)
	private String phone;

	@Column(columnDefinition = "text")
	private String address;

	@Column(name = "contact_person", length = 100)
	private String contactPerson;

	@ManyToMany
	@JoinTable(name = "supplier_brands",
			joinColumns = @JoinColumn(name = "supplier_id"),
			inverseJoinColumns = @JoinColumn(name = "brand_id"))
	private Set<Brand> brands = new HashSet<>();
}

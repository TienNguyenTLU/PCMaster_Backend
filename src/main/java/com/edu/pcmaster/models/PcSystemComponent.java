package com.edu.pcmaster.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "pc_system_components")
public class PcSystemComponent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "component_product_id", nullable = false)
    private Product componentProduct;

    @Column(nullable = false)
    private int quantity = 1;
}

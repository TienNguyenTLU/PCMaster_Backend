package com.edu.pcmaster.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "import_tickets")
public class ImportTicket {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String ticketCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_id", nullable = false)
    private Supplier supplier;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @Column(precision = 19, scale = 4)
    private BigDecimal totalCost;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @Column(columnDefinition = "text")
    private String notes;

    @OneToMany(mappedBy = "importTicket", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ImportDetail> importDetails = new ArrayList<>();
}

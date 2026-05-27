package com.edu.pcmaster.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "pc_system_details")
public class PcSystemDetail {

    @Id
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "id")
    @com.fasterxml.jackson.annotation.JsonIgnore
    private Product product;

    @OneToMany(mappedBy = "pcSystemDetail", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PcSystemComponent> components = new ArrayList<>();
}

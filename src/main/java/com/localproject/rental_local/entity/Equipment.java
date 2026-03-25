package com.localproject.rental_local.entity;

import com.localproject.rental_local.enums.EquipmentCategory;
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
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "equipment")
public class Equipment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EquipmentCategory category;

    @Column(name = "daily_rate", nullable = false, precision = 10, scale = 2)
    private BigDecimal dailyRate;

    @Builder.Default
    @Column(name = "is_available", nullable = false, columnDefinition = "BIT(1) DEFAULT b'1'")
    private Boolean isAvailable = true;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "seller_id", nullable = false)
    private User seller;

    @Builder.Default
    @Column(name = "is_deleted", nullable = false, columnDefinition = "BIT(1) DEFAULT b'0'")
    private Boolean isDeleted = false;


    @Builder.Default
    @OneToMany(mappedBy = "equipment", fetch = FetchType.LAZY)
    private List<Rental> rentals = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "equipment", fetch = FetchType.LAZY)
    private List<Review> reviews = new ArrayList<>();
}


package com.project.shopapp.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

@Entity
@Table(name = "products")
public class Product extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 350)
    private String name;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @Column(length = 500)
    private String thumbnail;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProductImage> images = new ArrayList<>();

    protected Product() {}

    public Product(
            String name,
            BigDecimal price,
            String thumbnail,
            String description,
            Category category) {
        this.name = name;
        this.price = price;
        this.thumbnail = thumbnail;
        this.description = description;
        this.category = category;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public String getThumbnail() {
        return thumbnail;
    }

    public String getDescription() {
        return description;
    }

    public Category getCategory() {
        return category;
    }

    public void update(
            String name,
            BigDecimal price,
            String thumbnail,
            String description,
            Category category) {
        this.name = name;
        this.price = price;
        this.thumbnail = thumbnail;
        this.description = description;
        this.category = category;
    }
}

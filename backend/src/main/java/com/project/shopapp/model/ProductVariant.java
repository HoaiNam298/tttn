package com.project.shopapp.model;

import com.project.shopapp.exceptions.InsufficientStockException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.util.Locale;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@Table(name = "product_variants")
@Data
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class ProductVariant {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Product product;

    @Column(nullable = false, length = 64, unique = true)
    private String sku;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(nullable = false, length = 60)
    @Builder.Default
    private String color = "";

    @Column(nullable = false, length = 60)
    @Builder.Default
    private String size = "";

    @Column(nullable = false, length = 60)
    @Builder.Default
    private String capacity = "";

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @Column(nullable = false)
    private int stock;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    @Version private long version;

    public ProductVariant(Product product) {
        this();
        this.product = product;
    }

    public void update(
            String sku,
            String name,
            String color,
            String size,
            String capacity,
            BigDecimal price,
            int stock,
            String imageUrl,
            boolean active) {
        this.sku = sku.trim().toUpperCase(Locale.ROOT);
        this.name = name.trim();
        this.color = color == null ? "" : color.trim();
        this.size = size == null ? "" : size.trim();
        this.capacity = capacity == null ? "" : capacity.trim();
        this.price = price;
        this.stock = stock;
        this.imageUrl = imageUrl;
        this.active = active;
    }

    public void reserve(int quantity) {
        if (!active || quantity <= 0 || stock < quantity) {
            throw new InsufficientStockException(name, active ? stock : 0, quantity);
        }
        stock -= quantity;
    }

    public void release(int quantity) {
        stock = Math.addExact(stock, quantity);
    }
}

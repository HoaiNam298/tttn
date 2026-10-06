package com.project.shopapp.model;

import com.project.shopapp.exceptions.InsufficientStockException;
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
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
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

    @Column(nullable = false)
    private int stock = 100;

    @Version
    @Column(nullable = false)
    private long version;

    @Column(name = "inventory_revision", nullable = false)
    private long inventoryRevision;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL)
    @OrderBy("id ASC")
    private List<ProductVariant> variants = new ArrayList<>();

    public List<ProductVariant> getVariants() {
        return List.copyOf(variants);
    }

    public long getVersion() {
        return version;
    }

    public void addVariant(ProductVariant variant) {
        variants.add(variant);
    }

    public void changeInventory(Integer legacyStock, List<String> imageUrls) {
        if (variants.isEmpty()) {
            if (legacyStock == null) {
                throw new IllegalArgumentException(
                        "Stock is required for a product without variants");
            }
            stock = legacyStock;
        } else {
            refreshVariantSummary();
        }
        images.removeIf(image -> !imageUrls.contains(image.getImageUrl()));
        for (int index = 0; index < imageUrls.size(); index++) {
            String url = imageUrls.get(index);
            ProductImage image =
                    images.stream()
                            .filter(item -> item.getImageUrl().equals(url))
                            .findFirst()
                            .orElse(null);
            if (image == null) {
                image = new ProductImage(this, url);
                images.add(image);
            }
            image.setPosition(index);
        }
        thumbnail = imageUrls.isEmpty() ? null : imageUrls.get(0);
        inventoryRevision++;
    }

    public void refreshVariantSummary() {
        if (!variants.isEmpty()) {
            stock =
                    variants.stream()
                            .filter(ProductVariant::isActive)
                            .mapToInt(ProductVariant::getStock)
                            .reduce(0, Math::addExact);
            price =
                    variants.stream()
                            .filter(ProductVariant::isActive)
                            .map(ProductVariant::getPrice)
                            .min(BigDecimal::compareTo)
                            .orElse(price);
        }
    }

    public void reserve(int quantity, ProductVariant variant) {
        if (variant == null) {
            if (!variants.isEmpty()) {
                throw new IllegalStateException("Please select a product variant");
            }
            reserve(quantity);
        } else {
            variant.reserve(quantity);
            refreshVariantSummary();
            inventoryRevision++;
        }
    }

    public void release(int quantity, ProductVariant variant) {
        if (variant == null) {
            release(quantity);
        } else {
            variant.release(quantity);
            refreshVariantSummary();
            inventoryRevision++;
        }
    }

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
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

    public int getStock() {
        return stock;
    }

    public List<ProductImage> getImages() {
        return images.stream()
                .sorted(java.util.Comparator.comparingInt(ProductImage::getPosition))
                .toList();
    }

    public void reserve(int quantity) {
        if (quantity <= 0 || stock < quantity) {
            throw new InsufficientStockException(name, stock, quantity);
        }
        stock -= quantity;
    }

    public void release(int quantity) {
        stock += quantity;
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
        refreshVariantSummary();
    }
}

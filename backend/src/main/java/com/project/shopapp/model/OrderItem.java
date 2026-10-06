package com.project.shopapp.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;

@Entity
@Table(name = "order_items")
public class OrderItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "variant_id")
    private ProductVariant variant;

    @Column(name = "variant_name", length = 150)
    private String variantName;

    @Column(length = 64)
    private String sku;

    public ProductVariant getVariant() {
        return variant;
    }

    public Long getVariantId() {
        return variant == null ? null : variant.getId();
    }

    public String getVariantName() {
        return variantName;
    }

    public String getSku() {
        return sku;
    }

    @Column(name = "product_name", nullable = false, length = 350)
    private String productName;

    @Column(name = "unit_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPrice;

    @Column(nullable = false)
    private int quantity;

    @Column(name = "line_total", nullable = false, precision = 14, scale = 2)
    private BigDecimal lineTotal;

    protected OrderItem() {}

    OrderItem(Order order, Product product, int quantity) {
        this(order, product, null, quantity);
    }

    OrderItem(Order order, Product product, ProductVariant variant, int quantity) {
        this.order = order;
        this.product = product;
        this.productName = product.getName();
        this.variant = variant;
        this.variantName = variant == null ? null : variant.getName();
        this.sku = variant == null ? null : variant.getSku();
        this.unitPrice = variant == null ? product.getPrice() : variant.getPrice();
        this.quantity = quantity;
        this.lineTotal = unitPrice.multiply(BigDecimal.valueOf(quantity));
    }

    public Long getProductId() {
        return product.getId();
    }

    public Product getProduct() {
        return product;
    }

    public Long getId() {
        return id;
    }

    public Order getOrder() {
        return order;
    }

    public String getProductName() {
        return productName;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public int getQuantity() {
        return quantity;
    }

    public BigDecimal getLineTotal() {
        return lineTotal;
    }
}

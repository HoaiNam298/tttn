package com.project.shopapp.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "product_reviews")
public class ProductReview extends BaseEntity {
    @jakarta.persistence.Version private long version;

    @Column(name = "shop_reply", length = 1000)
    private String shopReply;

    @Column(name = "replied_at")
    private java.time.Instant repliedAt;

    @jakarta.persistence.ElementCollection
    @jakarta.persistence.CollectionTable(
            name = "review_images",
            joinColumns = @JoinColumn(name = "review_id"))
    @jakarta.persistence.OrderColumn(name = "position")
    @Column(name = "url", nullable = false, length = 500)
    @org.hibernate.annotations.BatchSize(size = 50)
    private java.util.List<String> images = new java.util.ArrayList<>();

    public void attachImages(java.util.List<String> images) {
        if (images.size() > 5) {
            throw new IllegalArgumentException("At most five review images are allowed");
        }
        this.images = new java.util.ArrayList<>(images);
    }

    public java.util.List<String> getImages() {
        return java.util.List.copyOf(images);
    }

    public void reply(String text, long expectedVersion) {
        if (version != expectedVersion) {
            throw new IllegalStateException("Review changed. Reload before replying");
        }
        shopReply = text.trim();
        repliedAt = java.time.Instant.now();
    }

    public Product getProduct() {
        return product;
    }

    public long getVersion() {
        return version;
    }

    public String getShopReply() {
        return shopReply;
    }

    public java.time.Instant getRepliedAt() {
        return repliedAt;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_item_id", nullable = false, unique = true)
    private OrderItem orderItem;

    @Column(nullable = false)
    private int rating;

    @Column(nullable = false, length = 1000)
    private String comment;

    protected ProductReview() {}

    public ProductReview(
            Product product, User user, OrderItem orderItem, int rating, String comment) {
        this.product = product;
        this.user = user;
        this.orderItem = orderItem;
        this.rating = rating;
        this.comment = comment;
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public int getRating() {
        return rating;
    }

    public String getComment() {
        return comment;
    }
}

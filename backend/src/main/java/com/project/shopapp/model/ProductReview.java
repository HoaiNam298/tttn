package com.project.shopapp.model;

import com.project.shopapp.components.MessageKeys;
import com.project.shopapp.exceptions.LocalizedArgumentException;
import com.project.shopapp.exceptions.LocalizedStateException;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.BatchSize;

@Entity
@Table(name = "product_reviews")
@Data
@Builder
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class ProductReview extends BaseEntity {
    @Version private long version;

    @Column(name = "shop_reply", length = 1000)
    private String shopReply;

    @Column(name = "replied_at")
    private Instant repliedAt;

    @ElementCollection
    @CollectionTable(name = "review_images", joinColumns = @JoinColumn(name = "review_id"))
    @OrderColumn(name = "position")
    @Column(name = "url", nullable = false, length = 500)
    @BatchSize(size = 50)
    @Builder.Default
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<String> images = new ArrayList<>();

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private User user;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_item_id", nullable = false, unique = true)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private OrderItem orderItem;

    @Column(nullable = false)
    private int rating;

    @Column(nullable = false, length = 1000)
    private String comment;

    public void attachImages(List<String> images) {
        if (images.size() > 5) {
            throw new LocalizedArgumentException(MessageKeys.REVIEW_IMAGE_LIMIT);
        }
        this.images = new ArrayList<>(images);
    }

    public List<String> getImages() {
        return List.copyOf(images);
    }

    public void reply(String text, long expectedVersion) {
        if (version != expectedVersion) {
            throw new LocalizedStateException(MessageKeys.REVIEW_CHANGED);
        }
        shopReply = text.trim();
        repliedAt = Instant.now();
    }

    public ProductReview(
            Product product, User user, OrderItem orderItem, int rating, String comment) {
        this();
        this.product = product;
        this.user = user;
        this.orderItem = orderItem;
        this.rating = rating;
        this.comment = comment;
    }
}

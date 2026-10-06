package com.project.shopapp.model;

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
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "order_status_history")
public class OrderStatusHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private OrderStatus status;

    @Column(nullable = false, length = 20)
    private String actor;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(nullable = false)
    private boolean imported;

    protected OrderStatusHistory() {}

    public OrderStatusHistory(Order order, OrderStatus status, String actor) {
        this.order = order;
        this.status = status;
        this.actor = actor;
        this.occurredAt = Instant.now();
    }

    public OrderStatus getStatus() {
        return status;
    }

    public String getActor() {
        return actor;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public boolean isImported() {
        return imported;
    }
}

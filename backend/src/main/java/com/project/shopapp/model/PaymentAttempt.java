package com.project.shopapp.model;

import com.project.shopapp.enums.PaymentStatus;
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
import java.math.BigDecimal;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@Table(name = "payment_attempts")
@Data
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class PaymentAttempt {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false, unique = true)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Order order;

    @Column(name = "txn_ref", nullable = false, unique = true, length = 64)
    private String txnRef;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal amount;

    @Column(name = "payment_url", nullable = false, columnDefinition = "text")
    private String paymentUrl;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private PaymentStatus status = PaymentStatus.PENDING;

    @Column(name = "provider_transaction_no", unique = true, length = 30)
    private String providerTransactionNo;

    @Column(name = "response_code", length = 2)
    private String responseCode;

    @Column(name = "paid_at")
    private Instant paidAt;

    public PaymentAttempt(Order order, String txnRef, String paymentUrl, Instant createdAt) {
        this();
        this.order = order;
        this.txnRef = txnRef;
        this.amount = order.getTotal();
        this.paymentUrl = paymentUrl;
        this.createdAt = createdAt;
        this.expiresAt = order.getPaymentExpiresAt();
    }

    public Long getOrderId() {
        return order.getId();
    }

    public void finish(
            PaymentStatus status, String transactionNo, String responseCode, Instant paidAt) {
        this.status = status;
        this.providerTransactionNo = transactionNo;
        this.responseCode = responseCode;
        this.paidAt = paidAt;
    }
}

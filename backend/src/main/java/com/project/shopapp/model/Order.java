package com.project.shopapp.model;

import jakarta.persistence.CascadeType;
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
import java.util.UUID;

@Entity
@Table(name = "orders")
public class Order extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_number", nullable = false, unique = true)
    private UUID orderNumber;

    @Column(name = "request_id")
    private UUID requestId;

    @Column(name = "request_fingerprint", length = 64)
    private String requestFingerprint;

    public void identifyRequest(UUID requestId, String fingerprint) {
        this.requestId = requestId;
        this.requestFingerprint = fingerprint;
    }

    public String getRequestFingerprint() {
        return requestFingerprint;
    }

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "recipient_name", nullable = false, length = 100)
    private String recipientName;

    @Column(name = "phone_number", nullable = false, length = 20)
    private String phoneNumber;

    @Column(name = "shipping_address", nullable = false, length = 255)
    private String shippingAddress;

    @Column(nullable = false, length = 500)
    private String note;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private OrderStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false, length = 20)
    private PaymentMethod paymentMethod;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal subtotal;

    @Column(name = "shipping_fee", nullable = false, precision = 14, scale = 2)
    private BigDecimal shippingFee;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal total;

    @Enumerated(EnumType.STRING)
    @Column(name = "shipping_method", nullable = false, length = 20)
    private ShippingMethod shippingMethod = ShippingMethod.STANDARD;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "voucher_id")
    private Voucher voucher;

    @Column(name = "voucher_code", length = 40)
    private String voucherCode;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal discount = BigDecimal.ZERO;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL)
    @jakarta.persistence.OrderBy("occurredAt ASC, id ASC")
    private List<OrderStatusHistory> history = new ArrayList<>();

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();

    protected Order() {}

    public Order(
            User user,
            String recipientName,
            String phoneNumber,
            String shippingAddress,
            String note,
            BigDecimal shippingFee) {
        this.orderNumber = UUID.randomUUID();
        this.user = user;
        this.recipientName = recipientName;
        this.phoneNumber = phoneNumber;
        this.shippingAddress = shippingAddress;
        this.note = note;
        this.status = OrderStatus.PENDING;
        this.paymentMethod = PaymentMethod.COD;
        this.subtotal = BigDecimal.ZERO;
        this.shippingFee = shippingFee;
        this.total = shippingFee;
        history.add(new OrderStatusHistory(this, status, "CUSTOMER"));
    }

    public void addItem(Product product, int quantity) {
        addItem(product, null, quantity);
    }

    public void addItem(Product product, ProductVariant variant, int quantity) {
        OrderItem item = new OrderItem(this, product, variant, quantity);
        items.add(item);
        subtotal = subtotal.add(item.getLineTotal());
        total = subtotal.add(shippingFee);
    }

    public void updateStatus(OrderStatus nextStatus) {
        updateStatus(nextStatus, "ADMIN");
    }

    public void updateStatus(OrderStatus nextStatus, String actor) {
        if (!status.canTransitionTo(nextStatus)) {
            throw new IllegalStateException(
                    "Cannot change order status from " + status + " to " + nextStatus);
        }
        status = nextStatus;
        history.add(new OrderStatusHistory(this, status, actor));
    }

    public void selectShipping(ShippingMethod method) {
        shippingMethod = method;
        shippingFee = method.getFee();
        total = subtotal.add(shippingFee).subtract(discount);
    }

    public void applyVoucher(Voucher voucher) {
        this.voucher = voucher;
        voucherCode = voucher.getCode();
        discount = voucher.calculateDiscount(subtotal);
        total = subtotal.add(shippingFee).subtract(discount);
    }

    public ShippingMethod getShippingMethod() {
        return shippingMethod;
    }

    public Voucher getVoucher() {
        return voucher;
    }

    public String getVoucherCode() {
        return voucherCode;
    }

    public BigDecimal getDiscount() {
        return discount;
    }

    public List<OrderStatusHistory> getHistory() {
        return List.copyOf(history);
    }

    public Long getId() {
        return id;
    }

    public UUID getOrderNumber() {
        return orderNumber;
    }

    public User getUser() {
        return user;
    }

    public String getRecipientName() {
        return recipientName;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public String getShippingAddress() {
        return shippingAddress;
    }

    public String getNote() {
        return note;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public BigDecimal getShippingFee() {
        return shippingFee;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public List<OrderItem> getItems() {
        return List.copyOf(items);
    }
}

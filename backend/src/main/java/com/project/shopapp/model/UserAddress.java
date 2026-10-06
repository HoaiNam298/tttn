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

@Entity
@Table(name = "user_addresses")
public class UserAddress {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "recipient_name", nullable = false, length = 100)
    private String recipientName;

    @Column(name = "phone_number", nullable = false, length = 20)
    private String phoneNumber;

    @Column(name = "shipping_address", nullable = false, length = 255)
    private String shippingAddress;

    @Column(name = "is_default", nullable = false)
    private boolean defaultAddress;

    protected UserAddress() {}

    public UserAddress(User user) {
        this.user = user;
    }

    public void update(String name, String phone, String address) {
        this.recipientName = name.trim();
        this.phoneNumber = phone.trim();
        this.shippingAddress = address.trim();
    }

    public void setDefaultAddress(boolean value) {
        this.defaultAddress = value;
    }

    public Long getId() {
        return id;
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

    public boolean isDefaultAddress() {
        return defaultAddress;
    }
}

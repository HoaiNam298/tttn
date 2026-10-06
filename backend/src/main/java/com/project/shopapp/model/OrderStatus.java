package com.project.shopapp.model;

public enum OrderStatus {
    PENDING,
    CONFIRMED,
    SHIPPING,
    DELIVERED,
    COMPLETED,
    CANCELLED;

    public boolean canTransitionTo(OrderStatus next) {
        return switch (this) {
            case PENDING -> next == CONFIRMED || next == CANCELLED;
            case CONFIRMED -> next == SHIPPING || next == CANCELLED;
            case SHIPPING -> next == DELIVERED;
            case DELIVERED -> next == COMPLETED;
            case COMPLETED, CANCELLED -> false;
        };
    }
}

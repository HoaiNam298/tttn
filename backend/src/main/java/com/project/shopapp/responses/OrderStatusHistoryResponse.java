package com.project.shopapp.responses;

import com.project.shopapp.model.OrderStatus;
import com.project.shopapp.model.OrderStatusHistory;
import java.time.Instant;

public record OrderStatusHistoryResponse(
        OrderStatus status, String actor, Instant occurredAt, boolean imported) {
    public static OrderStatusHistoryResponse from(OrderStatusHistory item) {
        return new OrderStatusHistoryResponse(
                item.getStatus(), item.getActor(), item.getOccurredAt(), item.isImported());
    }
}

package com.project.shopapp.dtos.response;

import com.project.shopapp.enums.OrderStatus;
import com.project.shopapp.model.OrderStatusHistory;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderStatusHistoryResponse {

    private OrderStatus status;

    private String actor;

    private Instant occurredAt;

    private boolean imported;

    public static OrderStatusHistoryResponse from(OrderStatusHistory item) {
        return OrderStatusHistoryResponse.builder()
                .status(item.getStatus())
                .actor(item.getActor())
                .occurredAt(item.getOccurredAt())
                .imported(item.isImported())
                .build();
    }
}

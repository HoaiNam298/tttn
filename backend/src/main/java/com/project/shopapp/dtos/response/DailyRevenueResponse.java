package com.project.shopapp.dtos.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DailyRevenueResponse {

    private LocalDate date;

    private long completedOrders;

    private BigDecimal revenue;
}

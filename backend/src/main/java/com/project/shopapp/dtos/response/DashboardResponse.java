package com.project.shopapp.dtos.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardResponse {

    private LocalDate from;

    private LocalDate to;

    private String timezone;

    private BigDecimal revenue;

    private long completedOrders;

    private long estimatedCompletionCount;

    private long productCount;

    private long categoryCount;

    private long orderCount;

    private Map<String, Long> statusCounts;

    private List<DailyRevenueResponse> dailyRevenue;

    private List<TopProductResponse> topProducts;

    private List<OrderSummaryResponse> recentOrders;
}

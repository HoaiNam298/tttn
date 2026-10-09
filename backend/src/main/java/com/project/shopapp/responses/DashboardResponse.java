package com.project.shopapp.responses;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public record DashboardResponse(
        LocalDate from,
        LocalDate to,
        String timezone,
        BigDecimal revenue,
        long completedOrders,
        long estimatedCompletionCount,
        long productCount,
        long categoryCount,
        long orderCount,
        Map<String, Long> statusCounts,
        List<DailyRevenueResponse> dailyRevenue,
        List<TopProductResponse> topProducts,
        List<OrderSummaryResponse> recentOrders) {}

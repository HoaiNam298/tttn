package com.project.shopapp.projections;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface DailyRevenueProjection {
    LocalDate getDay();

    long getOrders();

    BigDecimal getRevenue();
}

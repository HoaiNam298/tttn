package com.project.shopapp.responses;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DailyRevenueResponse(LocalDate date, long completedOrders, BigDecimal revenue) {}

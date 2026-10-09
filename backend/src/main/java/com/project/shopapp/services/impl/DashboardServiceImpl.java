package com.project.shopapp.services.impl;

import com.project.shopapp.repositories.CategoryRepository;
import com.project.shopapp.repositories.DashboardRepository;
import com.project.shopapp.repositories.OrderRepository;
import com.project.shopapp.repositories.ProductRepository;
import com.project.shopapp.responses.DailyRevenueResponse;
import com.project.shopapp.responses.DashboardResponse;
import com.project.shopapp.responses.OrderSummaryResponse;
import com.project.shopapp.services.DashboardService;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DashboardServiceImpl implements DashboardService {
    private static final ZoneId REPORT_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private final DashboardRepository dashboard;
    private final ProductRepository products;
    private final CategoryRepository categories;
    private final OrderRepository orders;

    public DashboardServiceImpl(
            DashboardRepository dashboard,
            ProductRepository products,
            CategoryRepository categories,
            OrderRepository orders) {
        this.dashboard = dashboard;
        this.products = products;
        this.categories = categories;
        this.orders = orders;
    }

    @Override
    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public DashboardResponse report(LocalDate from, LocalDate to) {
        LocalDate endDate = to == null ? LocalDate.now(REPORT_ZONE) : to;
        LocalDate startDate = from == null ? endDate.minusDays(29) : from;
        long days = ChronoUnit.DAYS.between(startDate, endDate) + 1;
        if (days < 1 || days > 93) {
            throw new IllegalArgumentException("Report range must contain 1 to 93 days");
        }
        Instant start = startDate.atStartOfDay(REPORT_ZONE).toInstant();
        Instant end = endDate.plusDays(1).atStartOfDay(REPORT_ZONE).toInstant();
        Map<LocalDate, DailyRevenueResponse> actual =
                dashboard.dailyRevenue(start, end).stream()
                        .collect(Collectors.toMap(DailyRevenueResponse::date, Function.identity()));
        var daily = new ArrayList<DailyRevenueResponse>();
        BigDecimal revenue = BigDecimal.ZERO;
        long completed = 0;
        for (LocalDate day = startDate; !day.isAfter(endDate); day = day.plusDays(1)) {
            DailyRevenueResponse value =
                    actual.getOrDefault(day, new DailyRevenueResponse(day, 0, BigDecimal.ZERO));
            daily.add(value);
            revenue = revenue.add(value.revenue());
            completed += value.completedOrders();
        }
        return new DashboardResponse(
                startDate,
                endDate,
                REPORT_ZONE.getId(),
                revenue,
                completed,
                dashboard.estimatedCompletionCount(start, end),
                products.count(),
                categories.count(),
                orders.count(),
                dashboard.statusCounts(),
                daily,
                dashboard.topProducts(start, end),
                orders.findAll(
                                PageRequest.of(
                                        0, 5, Sort.by(Sort.Direction.DESC, "createdAt", "id")))
                        .map(OrderSummaryResponse::from)
                        .getContent());
    }
}

package com.project.shopapp.services.impl;

import com.project.shopapp.dtos.response.DailyRevenueResponse;
import com.project.shopapp.dtos.response.DashboardResponse;
import com.project.shopapp.dtos.response.OrderSummaryResponse;
import com.project.shopapp.dtos.response.TopProductResponse;
import com.project.shopapp.projections.DailyRevenueProjection;
import com.project.shopapp.projections.TopProductProjection;
import com.project.shopapp.repositories.CategoryRepository;
import com.project.shopapp.repositories.DashboardRepository;
import com.project.shopapp.repositories.OrderRepository;
import com.project.shopapp.repositories.ProductRepository;
import com.project.shopapp.services.DashboardService;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
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
        var actual =
                dailyRevenue(start, end).stream()
                        .collect(
                                Collectors.toMap(
                                        DailyRevenueResponse::getDate, Function.identity()));
        var daily = new ArrayList<DailyRevenueResponse>();
        BigDecimal revenue = BigDecimal.ZERO;
        long completed = 0;
        for (LocalDate day = startDate; !day.isAfter(endDate); day = day.plusDays(1)) {
            DailyRevenueResponse value =
                    actual.getOrDefault(day, new DailyRevenueResponse(day, 0, BigDecimal.ZERO));
            daily.add(value);
            revenue = revenue.add(value.getRevenue());
            completed += value.getCompletedOrders();
        }
        return DashboardResponse.builder()
                .from(startDate)
                .to(endDate)
                .timezone(REPORT_ZONE.getId())
                .revenue(revenue)
                .completedOrders(completed)
                .estimatedCompletionCount(dashboard.findEstimatedCompletionCount(start, end))
                .productCount(products.count())
                .categoryCount(categories.count())
                .orderCount(orders.count())
                .statusCounts(statusCounts())
                .dailyRevenue(daily)
                .topProducts(topProducts(start, end))
                .recentOrders(recentOrders())
                .build();
    }

    private List<DailyRevenueResponse> dailyRevenue(Instant start, Instant end) {
        return dashboard.findDailyRevenue(start, end).stream()
                .map(this::toDailyRevenueResponse)
                .toList();
    }

    private DailyRevenueResponse toDailyRevenueResponse(DailyRevenueProjection row) {
        return DailyRevenueResponse.builder()
                .date(row.getDay())
                .completedOrders(row.getOrders())
                .revenue(row.getRevenue())
                .build();
    }

    private List<TopProductResponse> topProducts(Instant start, Instant end) {
        return dashboard.findTopProducts(start, end).stream()
                .map(this::toTopProductResponse)
                .toList();
    }

    private TopProductResponse toTopProductResponse(TopProductProjection row) {
        return TopProductResponse.builder()
                .productId(row.getProductId())
                .productName(row.getName())
                .quantity(row.getQuantity())
                .grossSales(row.getGross())
                .build();
    }

    private Map<String, Long> statusCounts() {
        Map<String, Long> result = new LinkedHashMap<>();
        dashboard.findStatusCounts().forEach(row -> result.put(row.getStatus(), row.getTotal()));
        return result;
    }

    private List<OrderSummaryResponse> recentOrders() {
        var pageable = PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "createdAt", "id"));
        return orders.findAll(pageable).map(OrderSummaryResponse::from).getContent();
    }
}

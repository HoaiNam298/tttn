package com.project.shopapp.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.project.shopapp.repositories.CategoryRepository;
import com.project.shopapp.repositories.DashboardRepository;
import com.project.shopapp.repositories.OrderRepository;
import com.project.shopapp.repositories.ProductRepository;
import com.project.shopapp.responses.DailyRevenueResponse;
import com.project.shopapp.services.impl.DashboardServiceImpl;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {
    @Mock DashboardRepository dashboard;
    @Mock ProductRepository products;
    @Mock CategoryRepository categories;
    @Mock OrderRepository orders;
    private DashboardService service;

    @BeforeEach
    void setup() {
        service = new DashboardServiceImpl(dashboard, products, categories, orders);
    }

    @Test
    void rejectsInvertedDateRangeWithoutQuerying() {
        assertThrows(
                IllegalArgumentException.class,
                () -> service.report(LocalDate.of(2026, 10, 9), LocalDate.of(2026, 10, 8)));
        verifyNoInteractions(dashboard);
    }

    @Test
    void rejectsUnboundedReport() {
        assertThrows(
                IllegalArgumentException.class,
                () -> service.report(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 10, 9)));
        verifyNoInteractions(dashboard);
    }

    @Test
    void fillsMissingDaysAndUsesVietnamDayBoundaries() {
        when(dashboard.dailyRevenue(any(), any()))
                .thenReturn(
                        List.of(
                                new DailyRevenueResponse(
                                        LocalDate.of(2026, 10, 9), 2, new BigDecimal("150000"))));
        when(orders.findAll(any(Pageable.class))).thenReturn(Page.empty());
        var report = service.report(LocalDate.of(2026, 10, 8), LocalDate.of(2026, 10, 9));
        assertEquals(2, report.dailyRevenue().size());
        assertEquals(BigDecimal.ZERO, report.dailyRevenue().get(0).revenue());
        assertEquals(new BigDecimal("150000"), report.revenue());
        assertEquals(2, report.completedOrders());
        verify(dashboard)
                .dailyRevenue(
                        Instant.parse("2026-10-07T17:00:00Z"),
                        Instant.parse("2026-10-09T17:00:00Z"));
    }
}

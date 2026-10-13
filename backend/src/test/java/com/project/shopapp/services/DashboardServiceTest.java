package com.project.shopapp.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.project.shopapp.dtos.response.TopProductResponse;
import com.project.shopapp.projections.DailyRevenueProjection;
import com.project.shopapp.projections.StatusCountProjection;
import com.project.shopapp.projections.TopProductProjection;
import com.project.shopapp.repositories.CategoryRepository;
import com.project.shopapp.repositories.DashboardRepository;
import com.project.shopapp.repositories.OrderRepository;
import com.project.shopapp.repositories.ProductRepository;
import com.project.shopapp.services.impl.DashboardServiceImpl;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
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
        DailyRevenueProjection daily = mock(DailyRevenueProjection.class);
        when(daily.getDay()).thenReturn(LocalDate.of(2026, 10, 9));
        when(daily.getOrders()).thenReturn(2L);
        when(daily.getRevenue()).thenReturn(new BigDecimal("150000"));
        when(dashboard.findDailyRevenue(any(), any())).thenReturn(List.of(daily));

        TopProductProjection product = mock(TopProductProjection.class);
        when(product.getProductId()).thenReturn(8L);
        when(product.getName()).thenReturn("Phone");
        when(product.getQuantity()).thenReturn(3L);
        when(product.getGross()).thenReturn(new BigDecimal("160000"));
        when(dashboard.findTopProducts(any(), any())).thenReturn(List.of(product));

        StatusCountProjection status = mock(StatusCountProjection.class);
        when(status.getStatus()).thenReturn("COMPLETED");
        when(status.getTotal()).thenReturn(2L);
        when(dashboard.findStatusCounts()).thenReturn(List.of(status));
        when(dashboard.findEstimatedCompletionCount(any(), any())).thenReturn(1L);
        when(orders.findAll(any(Pageable.class))).thenReturn(Page.empty());
        var report = service.report(LocalDate.of(2026, 10, 8), LocalDate.of(2026, 10, 9));
        assertEquals(2, report.getDailyRevenue().size());
        assertEquals(BigDecimal.ZERO, report.getDailyRevenue().get(0).getRevenue());
        assertEquals(new BigDecimal("150000"), report.getRevenue());
        assertEquals(2, report.getCompletedOrders());
        assertEquals(1, report.getEstimatedCompletionCount());
        assertEquals(Map.of("COMPLETED", 2L), report.getStatusCounts());
        assertEquals(
                List.of(new TopProductResponse(8L, "Phone", 3L, new BigDecimal("160000"))),
                report.getTopProducts());
        verify(dashboard)
                .findDailyRevenue(
                        Instant.parse("2026-10-07T17:00:00Z"),
                        Instant.parse("2026-10-09T17:00:00Z"));
    }
}

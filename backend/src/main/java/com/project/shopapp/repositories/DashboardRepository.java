package com.project.shopapp.repositories;

import com.project.shopapp.model.Order;
import com.project.shopapp.projections.DailyRevenueProjection;
import com.project.shopapp.projections.StatusCountProjection;
import com.project.shopapp.projections.TopProductProjection;
import com.project.shopapp.responses.DailyRevenueResponse;
import com.project.shopapp.responses.TopProductResponse;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

public interface DashboardRepository extends Repository<Order, Long> {
    @Query(
            value =
                    "SELECT (completed_at AT TIME ZONE 'Asia/Ho_Chi_Minh')::date AS day, COUNT(*) AS orders, SUM(subtotal - discount) AS revenue FROM orders WHERE status = 'COMPLETED' AND completed_at >= :start AND completed_at < :end GROUP BY day ORDER BY day",
            nativeQuery = true)
    List<DailyRevenueProjection> findDailyRevenue(Instant start, Instant end);

    @Query(
            value =
                    "SELECT COUNT(*) FROM orders WHERE status = 'COMPLETED' AND completion_time_estimated = TRUE AND completed_at >= :start AND completed_at < :end",
            nativeQuery = true)
    long findEstimatedCompletionCount(Instant start, Instant end);

    @Query(
            value =
                    "SELECT i.product_id AS productId, p.name AS name, SUM(i.quantity) AS quantity, SUM(i.line_total) AS gross FROM order_items i JOIN orders o ON o.id = i.order_id JOIN products p ON p.id = i.product_id WHERE o.status = 'COMPLETED' AND o.completed_at >= :start AND o.completed_at < :end GROUP BY i.product_id, p.name ORDER BY quantity DESC, i.product_id ASC LIMIT 5",
            nativeQuery = true)
    List<TopProductProjection> findTopProducts(Instant start, Instant end);

    @Query(
            value = "SELECT status AS status, COUNT(*) AS total FROM orders GROUP BY status",
            nativeQuery = true)
    List<StatusCountProjection> findStatusCounts();

    default List<DailyRevenueResponse> dailyRevenue(Instant start, Instant end) {
        return findDailyRevenue(start, end).stream()
                .map(
                        row ->
                                new DailyRevenueResponse(
                                        row.getDay(), row.getOrders(), row.getRevenue()))
                .toList();
    }

    default long estimatedCompletionCount(Instant start, Instant end) {
        return findEstimatedCompletionCount(start, end);
    }

    default List<TopProductResponse> topProducts(Instant start, Instant end) {
        return findTopProducts(start, end).stream()
                .map(
                        row ->
                                new TopProductResponse(
                                        row.getProductId(),
                                        row.getName(),
                                        row.getQuantity(),
                                        row.getGross()))
                .toList();
    }

    default Map<String, Long> statusCounts() {
        Map<String, Long> result = new LinkedHashMap<>();
        findStatusCounts().forEach(row -> result.put(row.getStatus(), row.getTotal()));
        return result;
    }
}

package com.project.shopapp.repositories;

import com.project.shopapp.responses.DailyRevenueResponse;
import com.project.shopapp.responses.TopProductResponse;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class DashboardRepository {
    private final JdbcTemplate jdbc;

    public DashboardRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<DailyRevenueResponse> dailyRevenue(Instant start, Instant end) {
        return jdbc.query(
                """
                SELECT (completed_at AT TIME ZONE 'Asia/Ho_Chi_Minh')::date AS day,
                       COUNT(*) AS orders, SUM(subtotal - discount) AS revenue
                FROM orders WHERE status = 'COMPLETED' AND completed_at >= ? AND completed_at < ?
                GROUP BY day ORDER BY day
                """,
                (rs, row) ->
                        new DailyRevenueResponse(
                                rs.getDate("day").toLocalDate(),
                                rs.getLong("orders"),
                                rs.getBigDecimal("revenue")),
                Timestamp.from(start),
                Timestamp.from(end));
    }

    public long estimatedCompletionCount(Instant start, Instant end) {
        return jdbc.queryForObject(
                "SELECT COUNT(*) FROM orders WHERE status = 'COMPLETED' AND completion_time_estimated = TRUE AND completed_at >= ? AND completed_at < ?",
                Long.class,
                Timestamp.from(start),
                Timestamp.from(end));
    }

    public List<TopProductResponse> topProducts(Instant start, Instant end) {
        return jdbc.query(
                """
                SELECT i.product_id, p.name, SUM(i.quantity) AS quantity, SUM(i.line_total) AS gross
                FROM order_items i JOIN orders o ON o.id = i.order_id JOIN products p ON p.id = i.product_id
                WHERE o.status = 'COMPLETED' AND o.completed_at >= ? AND o.completed_at < ?
                GROUP BY i.product_id, p.name ORDER BY quantity DESC, i.product_id ASC LIMIT 5
                """,
                (rs, row) ->
                        new TopProductResponse(
                                rs.getLong("product_id"),
                                rs.getString("name"),
                                rs.getLong("quantity"),
                                rs.getBigDecimal("gross")),
                Timestamp.from(start),
                Timestamp.from(end));
    }

    public Map<String, Long> statusCounts() {
        Map<String, Long> result = new LinkedHashMap<>();
        jdbc.query(
                "SELECT status, COUNT(*) AS count FROM orders GROUP BY status",
                (org.springframework.jdbc.core.RowCallbackHandler)
                        rs -> result.put(rs.getString("status"), rs.getLong("count")));
        return result;
    }
}

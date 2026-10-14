package com.project.shopapp.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.project.shopapp.components.VnpayGateway;
import com.project.shopapp.dtos.request.CreateOrderRequest;
import com.project.shopapp.dtos.request.OrderItemRequest;
import com.project.shopapp.enums.OrderStatus;
import com.project.shopapp.enums.PaymentMethod;
import com.project.shopapp.enums.PaymentStatus;
import com.project.shopapp.model.Category;
import com.project.shopapp.model.Product;
import com.project.shopapp.model.User;
import com.project.shopapp.repositories.CategoryRepository;
import com.project.shopapp.repositories.OrderRepository;
import com.project.shopapp.repositories.PaymentAttemptRepository;
import com.project.shopapp.repositories.ProductRepository;
import com.project.shopapp.repositories.UserRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.LinkedMultiValueMap;

@SpringBootTest(
        properties = {
            "app.vnpay.enabled=true",
            "app.vnpay.tmn-code=TESTCODE",
            "app.vnpay.hash-secret=sandbox-test-secret",
            "app.vnpay.public-base-url=https://demo.example.com",
            "app.vnpay.expiry-job-enabled=false",
            "spring.datasource.url=${shopapp.payment-test-db-url:jdbc:h2:mem:vnpay;MODE=PostgreSQL;DB_CLOSE_DELAY=-1}",
            "spring.datasource.driver-class-name=${shopapp.payment-test-db-driver:org.h2.Driver}",
            "spring.datasource.username=${shopapp.payment-test-db-username:sa}",
            "spring.datasource.password=${shopapp.payment-test-db-password:}",
            "logging.level.root=WARN",
            "logging.level.org.springframework=WARN"
        })
@ActiveProfiles("test")
class VnpayTransactionTest {
    @Autowired OrderService orderService;
    @Autowired PaymentService payments;
    @Autowired VnpayGateway gateway;
    @Autowired ProductRepository products;
    @Autowired CategoryRepository categories;
    @Autowired UserRepository users;
    @Autowired OrderRepository orders;
    @Autowired PaymentAttemptRepository attempts;
    @Autowired PlatformTransactionManager transactionManager;
    @Autowired JdbcTemplate jdbc;
    private String phone;
    private Long productId;
    private UUID requestId;

    @BeforeEach
    void seed() {
        requestId = UUID.randomUUID();
        phone =
                "09"
                        + String.format(
                                "%08d",
                                java.util.concurrent.ThreadLocalRandom.current()
                                        .nextInt(100000000));
        new TransactionTemplate(transactionManager)
                .executeWithoutResult(
                        status -> {
                            users.save(
                                    new User("Customer", phone, "HCM", null, "vnpay-" + requestId));
                            Category category = categories.save(new Category("VNPAY-" + requestId));
                            productId =
                                    products.save(
                                                    new Product(
                                                            "Test phone",
                                                            new BigDecimal("100000"),
                                                            "",
                                                            "",
                                                            category))
                                            .getId();
                        });
    }

    private CreateOrderRequest request(BigDecimal expectedTotal) {
        return CreateOrderRequest.builder()
                .recipientName("Customer")
                .phoneNumber(phone)
                .shippingAddress("HCM")
                .note("")
                .items(List.of(OrderItemRequest.builder().productId(productId).quantity(1).build()))
                .requestId(requestId)
                .expectedTotal(expectedTotal)
                .paymentMethod(PaymentMethod.VNPAY)
                .build();
    }

    private LinkedMultiValueMap<String, String> callback(Long id) {
        var attempt = attempts.findByOrder_Id(id).orElseThrow();
        var params = new LinkedMultiValueMap<String, String>();
        params.set("vnp_TmnCode", "TESTCODE");
        params.set("vnp_TxnRef", attempt.getTxnRef());
        params.set("vnp_Amount", Long.toString(VnpayGateway.amount(attempt.getAmount())));
        params.set("vnp_ResponseCode", "00");
        params.set("vnp_TransactionStatus", "00");
        params.set("vnp_TransactionNo", Long.toString(System.nanoTime() % 1000000000000000L));
        params.set(
                "vnp_PayDate",
                DateTimeFormatter.ofPattern("yyyyMMddHHmmss")
                        .withZone(ZoneId.of("Asia/Ho_Chi_Minh"))
                        .format(Instant.now()));
        params.set("vnp_SecureHash", gateway.sign(params.toSingleValueMap()));
        return params;
    }

    @Test
    void persistsItemsAndStatusHistoryWithoutCascade() {
        CreateOrderRequest request = request(null);
        request.setPaymentMethod(PaymentMethod.COD);
        Long orderId = orderService.create(phone, request).getId();

        assertEquals(
                1L,
                jdbc.queryForObject(
                        "SELECT COUNT(*) FROM order_items WHERE order_id = ?",
                        Long.class,
                        orderId));
        assertEquals(
                1L,
                jdbc.queryForObject(
                        "SELECT COUNT(*) FROM order_status_history WHERE order_id = ?",
                        Long.class,
                        orderId));

        orderService.updateStatus(orderId, OrderStatus.CONFIRMED);
        orderService.updateStatus(orderId, OrderStatus.SHIPPING);
        orderService.updateStatus(orderId, OrderStatus.DELIVERED);
        orderService.confirmReceipt(orderId, phone);
        orderService.confirmReceipt(orderId, phone);

        var reloaded = orderService.findOwnedOrder(orderId, phone);
        assertEquals(OrderStatus.COMPLETED, reloaded.getStatus());
        assertEquals(PaymentStatus.PAID, reloaded.getPaymentStatus());
        assertEquals(1, reloaded.getItems().size());
        assertEquals(5, reloaded.getHistory().size());
        assertEquals(
                5L,
                jdbc.queryForObject(
                        "SELECT COUNT(*) FROM order_status_history WHERE order_id = ?",
                        Long.class,
                        orderId));
    }

    @Test
    void changedTotalRollsBackOrderAndStock() {
        assertThrows(
                IllegalStateException.class,
                () -> orderService.create(phone, request(BigDecimal.ONE)));
        assertEquals(100, products.findById(productId).orElseThrow().getStock());
        assertEquals(
                0L,
                jdbc.queryForObject(
                        "select count(*) from orders where request_id = ?", Long.class, requestId));
    }

    @Test
    void concurrentLinkAndIpnRequestsAreIdempotent() throws Exception {
        Long id = orderService.create(phone, request(null)).getId();
        var pool = Executors.newFixedThreadPool(2);
        try {
            var start = new CountDownLatch(1);
            var first =
                    pool.submit(
                            () -> {
                                start.await();
                                return payments.createLink(id, phone, "127.0.0.1");
                            });
            var second =
                    pool.submit(
                            () -> {
                                start.await();
                                return payments.createLink(id, phone, "127.0.0.1");
                            });
            start.countDown();
            assertEquals(
                    first.get(10, TimeUnit.SECONDS).getPaymentUrl(),
                    second.get(10, TimeUnit.SECONDS).getPaymentUrl());
            var result = callback(id);
            var notify = new CountDownLatch(1);
            var a =
                    pool.submit(
                            () -> {
                                notify.await();
                                return payments.receiveIpn(result).getRspCode();
                            });
            var b =
                    pool.submit(
                            () -> {
                                notify.await();
                                return payments.receiveIpn(result).getRspCode();
                            });
            notify.countDown();
            assertEquals(
                    Set.of("00", "02"),
                    Set.of(a.get(10, TimeUnit.SECONDS), b.get(10, TimeUnit.SECONDS)));
            assertEquals(PaymentStatus.PAID, orders.findById(id).orElseThrow().getPaymentStatus());
            assertEquals(99, products.findById(productId).orElseThrow().getStock());
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void expiredOrderRestoresStockExactlyOnce() {
        Long id = orderService.create(phone, request(null)).getId();
        jdbc.update(
                "update orders set payment_expires_at = ? where id = ?",
                java.sql.Timestamp.from(Instant.now().minusSeconds(5)),
                id);
        orderService.closeUnpaidPayment(id, PaymentStatus.EXPIRED);
        orderService.closeUnpaidPayment(id, PaymentStatus.EXPIRED);
        assertEquals(100, products.findById(productId).orElseThrow().getStock());
        assertEquals(PaymentStatus.EXPIRED, orders.findById(id).orElseThrow().getPaymentStatus());
    }

    @Test
    void concurrentExpiryJobsDoNotRestoreStockTwice() throws Exception {
        Long id = orderService.create(phone, request(null)).getId();
        jdbc.update(
                "update orders set payment_expires_at = ? where id = ?",
                java.sql.Timestamp.from(Instant.now().minusSeconds(5)),
                id);
        var pool = Executors.newFixedThreadPool(2);
        try {
            var start = new CountDownLatch(1);
            var first =
                    pool.submit(
                            () -> {
                                start.await();
                                orderService.closeUnpaidPayment(id, PaymentStatus.EXPIRED);
                                return true;
                            });
            var second =
                    pool.submit(
                            () -> {
                                start.await();
                                orderService.closeUnpaidPayment(id, PaymentStatus.EXPIRED);
                                return true;
                            });
            start.countDown();
            first.get(10, TimeUnit.SECONDS);
            second.get(10, TimeUnit.SECONDS);
            assertEquals(100, products.findById(productId).orElseThrow().getStock());
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void lateSuccessAfterCancellationIsRecordedWithoutRestoringTheOrder() {
        Long id = orderService.create(phone, request(null)).getId();
        payments.createLink(id, phone, "127.0.0.1");
        orderService.cancelOwnedOrder(id, phone);
        assertEquals("00", payments.receiveIpn(callback(id)).getRspCode());
        assertEquals(
                PaymentStatus.REVIEW_REQUIRED,
                orders.findById(id).orElseThrow().getPaymentStatus());
        assertEquals(100, products.findById(productId).orElseThrow().getStock());
    }
}

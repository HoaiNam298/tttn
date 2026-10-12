package com.project.shopapp.services.impl;

import com.project.shopapp.components.VnpayGateway;
import com.project.shopapp.configuration.VnpayProperties;
import com.project.shopapp.exceptions.ResourceNotFoundException;
import com.project.shopapp.model.Order;
import com.project.shopapp.model.OrderStatus;
import com.project.shopapp.model.PaymentAttempt;
import com.project.shopapp.model.PaymentMethod;
import com.project.shopapp.model.PaymentStatus;
import com.project.shopapp.repositories.OrderRepository;
import com.project.shopapp.repositories.PaymentAttemptRepository;
import com.project.shopapp.responses.PaymentLinkResponse;
import com.project.shopapp.responses.VnpayIpnResponse;
import com.project.shopapp.services.OrderService;
import com.project.shopapp.services.PaymentService;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.MultiValueMap;

@Service
public class PaymentServiceImpl implements PaymentService {
    private static final Logger LOG = LoggerFactory.getLogger(PaymentServiceImpl.class);
    private final OrderRepository orders;
    private final PaymentAttemptRepository attempts;
    private final VnpayGateway gateway;
    private final VnpayProperties properties;
    private final OrderService orderService;
    private final EntityManager entityManager;

    public PaymentServiceImpl(
            OrderRepository orders,
            PaymentAttemptRepository attempts,
            VnpayGateway gateway,
            VnpayProperties properties,
            OrderService orderService,
            EntityManager entityManager) {
        this.orders = orders;
        this.attempts = attempts;
        this.gateway = gateway;
        this.properties = properties;
        this.orderService = orderService;
        this.entityManager = entityManager;
    }

    @Override
    @Transactional
    public PaymentLinkResponse createLink(Long orderId, String phoneNumber, String ip) {
        properties.requireEnabled();
        Order order =
                orders.findByIdAndUserPhoneNumber(orderId, phoneNumber)
                        .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        if (order.getPaymentMethod() != PaymentMethod.VNPAY
                || order.getStatus() != OrderStatus.PENDING
                || order.getPaymentStatus() != PaymentStatus.PENDING
                || !order.getPaymentExpiresAt().isAfter(Instant.now())) {
            throw new IllegalStateException(
                    "Order is no longer available for payment. Check the order status");
        }
        PaymentAttempt attempt = attempts.findByOrder_Id(orderId).orElse(null);
        if (attempt == null) {
            Instant now = Instant.now();
            String ref = UUID.randomUUID().toString().replace("-", "");
            String url =
                    gateway.createUrl(
                            ref, orderId, order.getTotal(), now, order.getPaymentExpiresAt(), ip);
            attempt = attempts.save(new PaymentAttempt(order, ref, url, now));
            LOG.info("Created VNPAY Sandbox attempt for order {} ref {}", orderId, ref);
        }
        if (attempt.getStatus() != PaymentStatus.PENDING) {
            throw new IllegalStateException("Payment attempt is already processed");
        }
        return new PaymentLinkResponse(orderId, attempt.getPaymentUrl(), attempt.getExpiresAt());
    }

    @Override
    @Transactional
    public VnpayIpnResponse receiveIpn(MultiValueMap<String, String> params) {
        if (!gateway.verify(params)) {
            LOG.warn("Rejected VNPAY IPN with invalid signature or merchant");
            return new VnpayIpnResponse("97", "Invalid signature");
        }
        String ref = params.getFirst("vnp_TxnRef");
        PaymentAttempt mapped = attempts.findByTxnRef(ref).orElse(null);
        if (mapped == null) {
            return new VnpayIpnResponse("01", "Order not found");
        }
        // Every mutation locks the order first, including cancellation and expiration.
        Order order = orders.findWithItemsById(mapped.getOrderId()).orElseThrow();
        PaymentAttempt attempt = attempts.findByTxnRef(ref).orElseThrow();
        entityManager.refresh(attempt);
        String amount = params.getFirst("vnp_Amount");
        if (amount == null
                || !amount.matches("[0-9]{1,12}")
                || Long.parseLong(amount) != VnpayGateway.amount(attempt.getAmount())
                || attempt.getAmount().compareTo(order.getTotal()) != 0) {
            return new VnpayIpnResponse("04", "Invalid amount");
        }
        if (attempt.getStatus() == PaymentStatus.PAID
                || attempt.getStatus() == PaymentStatus.REVIEW_REQUIRED) {
            return new VnpayIpnResponse("02", "Already processed");
        }
        String code = params.getFirst("vnp_ResponseCode");
        String state = params.getFirst("vnp_TransactionStatus");
        String transactionNo = params.getFirst("vnp_TransactionNo");
        if (code == null
                || !code.matches("[0-9]{2}")
                || state == null
                || !state.matches("[0-9]{2}")
                || transactionNo == null
                || !transactionNo.matches("[0-9]{1,15}")) {
            return new VnpayIpnResponse("99", "Invalid result data");
        }
        boolean success = "00".equals(code) && "00".equals(state);
        boolean suspiciousDebit = "07".equals(code);
        if (!success && !suspiciousDebit && attempt.getStatus() == PaymentStatus.FAILED) {
            return new VnpayIpnResponse("02", "Already processed");
        }
        Instant paidAt = null;
        if (success || suspiciousDebit) {
            try {
                paidAt =
                        LocalDateTime.parse(
                                        params.getFirst("vnp_PayDate"),
                                        DateTimeFormatter.ofPattern("uuuuMMddHHmmss")
                                                .withResolverStyle(ResolverStyle.STRICT))
                                .atZone(ZoneId.of("Asia/Ho_Chi_Minh"))
                                .toInstant();
            } catch (RuntimeException exception) {
                return new VnpayIpnResponse("99", "Invalid payment date");
            }
            if (paidAt.isBefore(attempt.getCreatedAt().minusSeconds(1))
                    || paidAt.isAfter(Instant.now().plusSeconds(60))) {
                return new VnpayIpnResponse("99", "Invalid payment date");
            }
            boolean needsReview =
                    suspiciousDebit
                            || order.getStatus() == OrderStatus.CANCELLED
                            || order.getPaymentStatus() != PaymentStatus.PENDING
                            || paidAt.isAfter(attempt.getExpiresAt());
            if (needsReview && order.getStatus() == OrderStatus.PENDING) {
                orderService.closeUnpaidPayment(order.getId(), PaymentStatus.FAILED);
            }
            PaymentStatus result = needsReview ? PaymentStatus.REVIEW_REQUIRED : PaymentStatus.PAID;
            order.recordPayment(result);
            attempt.finish(result, transactionNo, code, paidAt);
            LOG.info("VNPAY result order {} ref {} status {}", order.getId(), ref, result);
        } else {
            attempt.finish(
                    PaymentStatus.FAILED,
                    "0".equals(transactionNo) ? null : transactionNo,
                    code,
                    null);
            orderService.closeUnpaidPayment(order.getId(), PaymentStatus.FAILED);
            LOG.info("VNPAY failed order {} ref {} code {}", order.getId(), ref, code);
        }
        // Flush before acknowledgement; controller only replies after the transaction commits.
        attempts.flush();
        return new VnpayIpnResponse("00", "Confirmed");
    }

    @Override
    @Transactional(readOnly = true)
    public String returnLocation(MultiValueMap<String, String> params) {
        if (gateway.verify(params)) {
            var attempt = attempts.findByTxnRef(params.getFirst("vnp_TxnRef"));
            if (attempt.isPresent()) {
                return properties.getPublicBaseUrl()
                        + "/orders/"
                        + attempt.get().getOrderId()
                        + "?payment=returned";
            }
        }
        return properties.getPublicBaseUrl() + "/orders?payment=invalid";
    }
}

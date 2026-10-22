package com.project.shopapp.services.impl;

import com.project.shopapp.components.MessageKeys;
import com.project.shopapp.components.TransactionLog;
import com.project.shopapp.components.VnpayGateway;
import com.project.shopapp.configuration.VnpayProperties;
import com.project.shopapp.dtos.response.PaymentLinkResponse;
import com.project.shopapp.dtos.response.VnpayIpnResponse;
import com.project.shopapp.enums.OrderStatus;
import com.project.shopapp.enums.PaymentMethod;
import com.project.shopapp.enums.PaymentStatus;
import com.project.shopapp.exceptions.LocalizedStateException;
import com.project.shopapp.exceptions.ResourceNotFoundException;
import com.project.shopapp.model.Order;
import com.project.shopapp.model.PaymentAttempt;
import com.project.shopapp.repositories.OrderRepository;
import com.project.shopapp.repositories.PaymentAttemptRepository;
import com.project.shopapp.services.OrderBusinessService;
import com.project.shopapp.services.OrderService;
import com.project.shopapp.services.PaymentService;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.MultiValueMap;

@Service
@Slf4j
public class PaymentServiceImpl implements PaymentService {
    private final OrderRepository orders;
    private final PaymentAttemptRepository attempts;
    private final VnpayGateway gateway;
    private final VnpayProperties properties;
    private final OrderService orderService;
    private final EntityManager entityManager;
    private final OrderBusinessService orderBusinessService;

    public PaymentServiceImpl(
            OrderRepository orders,
            PaymentAttemptRepository attempts,
            VnpayGateway gateway,
            VnpayProperties properties,
            OrderService orderService,
            EntityManager entityManager,
            OrderBusinessService orderBusinessService) {
        this.orders = orders;
        this.attempts = attempts;
        this.gateway = gateway;
        this.properties = properties;
        this.orderService = orderService;
        this.entityManager = entityManager;
        this.orderBusinessService = orderBusinessService;
    }

    @Override
    @Transactional
    public PaymentLinkResponse createLink(Long orderId, String phoneNumber, String ip) {
        properties.requireEnabled();
        Order order =
                orders.findByIdAndUserPhoneNumber(orderId, phoneNumber)
                        .orElseThrow(
                                () -> new ResourceNotFoundException(MessageKeys.ORDER_NOT_FOUND));
        if (order.getPaymentMethod() != PaymentMethod.VNPAY
                || order.getStatus() != OrderStatus.PENDING
                || order.getPaymentStatus() != PaymentStatus.PENDING
                || !order.getPaymentExpiresAt().isAfter(Instant.now())) {
            throw new LocalizedStateException(MessageKeys.PAYMENT_ORDER_UNAVAILABLE);
        }
        PaymentAttempt attempt = attempts.findByOrder_Id(orderId).orElse(null);
        if (attempt == null) {
            Instant now = Instant.now();
            String ref = UUID.randomUUID().toString().replace("-", "");
            String url =
                    gateway.createUrl(
                            ref, orderId, order.getTotal(), now, order.getPaymentExpiresAt(), ip);
            attempt = attempts.save(new PaymentAttempt(order, ref, url, now));
            TransactionLog.info(
                    log, "Created VNPAY Sandbox attempt for order {} ref {}", orderId, ref);
        }
        if (attempt.getStatus() != PaymentStatus.PENDING) {
            throw new LocalizedStateException(MessageKeys.PAYMENT_ALREADY_PROCESSED);
        }
        return new PaymentLinkResponse(orderId, attempt.getPaymentUrl(), attempt.getExpiresAt());
    }

    @Override
    @Transactional
    public VnpayIpnResponse receiveIpn(MultiValueMap<String, String> params) {
        if (!gateway.verify(params)) {
            log.warn("Rejected VNPAY IPN with invalid signature or merchant");
            return new VnpayIpnResponse("97", "Invalid signature");
        }
        String ref = params.getFirst("vnp_TxnRef");
        PaymentAttempt mapped = attempts.findByTxnRef(ref).orElse(null);
        if (mapped == null) {
            log.warn("VNPAY IPN rejected reason=unknown_transaction");
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
            log.warn(
                    "VNPAY IPN rejected orderId={} txnRef={} reason=invalid_amount",
                    order.getId(),
                    ref);
            return new VnpayIpnResponse("04", "Invalid amount");
        }
        if (attempt.getStatus() == PaymentStatus.PAID
                || attempt.getStatus() == PaymentStatus.REVIEW_REQUIRED) {
            log.debug(
                    "VNPAY IPN ignored orderId={} txnRef={} reason=already_processed",
                    order.getId(),
                    ref);
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
            log.warn(
                    "VNPAY IPN rejected orderId={} txnRef={} reason=invalid_result_data",
                    order.getId(),
                    ref);
            return new VnpayIpnResponse("99", "Invalid result data");
        }
        boolean success = "00".equals(code) && "00".equals(state);
        boolean suspiciousDebit = "07".equals(code);
        if (!success && !suspiciousDebit && attempt.getStatus() == PaymentStatus.FAILED) {
            log.debug(
                    "VNPAY IPN ignored orderId={} txnRef={} reason=already_processed",
                    order.getId(),
                    ref);
            return new VnpayIpnResponse("02", "Already processed");
        }
        Instant paidAt = null;
        if (success || suspiciousDebit) {
            try {
                DateTimeFormatter formatter =
                        DateTimeFormatter.ofPattern("uuuuMMddHHmmss")
                                .withResolverStyle(ResolverStyle.STRICT);
                LocalDateTime paymentDate =
                        LocalDateTime.parse(params.getFirst("vnp_PayDate"), formatter);
                paidAt = paymentDate.atZone(ZoneId.of("Asia/Ho_Chi_Minh")).toInstant();
            } catch (RuntimeException exception) {
                log.warn(
                        "VNPAY IPN rejected orderId={} txnRef={} reason=invalid_payment_date",
                        order.getId(),
                        ref);
                return new VnpayIpnResponse("99", "Invalid payment date");
            }
            if (paidAt.isBefore(attempt.getCreatedAt().minusSeconds(1))
                    || paidAt.isAfter(Instant.now().plusSeconds(60))) {
                log.warn(
                        "VNPAY IPN rejected orderId={} txnRef={} reason=invalid_payment_date",
                        order.getId(),
                        ref);
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
            orderBusinessService.recordPayment(order, result);
            attempt.finish(result, transactionNo, code, paidAt);
            if (needsReview) {
                TransactionLog.warn(
                        log,
                        "VNPAY payment requires review orderId={} txnRef={} status={}",
                        order.getId(),
                        ref,
                        result);
            } else {
                TransactionLog.info(
                        log,
                        "VNPAY payment confirmed orderId={} txnRef={} status={}",
                        order.getId(),
                        ref,
                        result);
            }
        } else {
            attempt.finish(
                    PaymentStatus.FAILED,
                    "0".equals(transactionNo) ? null : transactionNo,
                    code,
                    null);
            orderService.closeUnpaidPayment(order.getId(), PaymentStatus.FAILED);
            TransactionLog.info(
                    log,
                    "VNPAY payment failed orderId={} txnRef={} code={}",
                    order.getId(),
                    ref,
                    code);
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
                log.debug("VNPAY browser return accepted orderId={}", attempt.get().getOrderId());
                return properties.getPublicBaseUrl()
                        + "/orders/"
                        + attempt.get().getOrderId()
                        + "?payment=returned";
            }
        }
        log.warn("VNPAY browser return rejected reason=invalid_signature_or_unknown_transaction");
        return properties.getPublicBaseUrl() + "/orders?payment=invalid";
    }
}

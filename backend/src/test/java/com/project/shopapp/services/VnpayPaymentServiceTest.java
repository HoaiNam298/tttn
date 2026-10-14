package com.project.shopapp.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.project.shopapp.components.VnpayGateway;
import com.project.shopapp.configuration.VnpayProperties;
import com.project.shopapp.enums.OrderStatus;
import com.project.shopapp.enums.PaymentStatus;
import com.project.shopapp.exceptions.ResourceNotFoundException;
import com.project.shopapp.model.Order;
import com.project.shopapp.model.PaymentAttempt;
import com.project.shopapp.model.User;
import com.project.shopapp.repositories.OrderRepository;
import com.project.shopapp.repositories.PaymentAttemptRepository;
import com.project.shopapp.services.impl.OrderBusinessServiceImpl;
import com.project.shopapp.services.impl.PaymentServiceImpl;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.LinkedMultiValueMap;

@ExtendWith(MockitoExtension.class)
class VnpayPaymentServiceTest {
    private final OrderBusinessService orderBusinessService = new OrderBusinessServiceImpl();
    @Mock OrderRepository orders;
    @Mock PaymentAttemptRepository attempts;
    @Mock OrderService orderService;
    @Mock EntityManager entityManager;
    private final VnpayProperties properties =
            new VnpayProperties(
                    true, "TESTCODE", "sandbox-test-secret", "https://demo.example.com");
    private final VnpayGateway gateway = new VnpayGateway(properties);
    private PaymentService service;
    private Order order;
    private PaymentAttempt attempt;

    @BeforeEach
    void setup() {
        service =
                new PaymentServiceImpl(
                        orders,
                        attempts,
                        gateway,
                        properties,
                        orderService,
                        entityManager,
                        new OrderBusinessServiceImpl());
        order =
                orderBusinessService.createOrder(
                        new User("Customer", "0900000000", "HCM", null, "test-subject"),
                        "Customer",
                        "0900000000",
                        "HCM",
                        "",
                        BigDecimal.valueOf(30000));
        ReflectionTestUtils.setField(order, "id", 7L);
        orderBusinessService.selectOnlinePayment(order, Instant.now().plusSeconds(900));
        attempt =
                new PaymentAttempt(
                        order,
                        "test-reference",
                        "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html?test",
                        Instant.now().minusSeconds(10));
    }

    private LinkedMultiValueMap<String, String> callback(String code) {
        var params = new LinkedMultiValueMap<String, String>();
        params.set("vnp_TmnCode", "TESTCODE");
        params.set("vnp_TxnRef", attempt.getTxnRef());
        params.set("vnp_Amount", "3000000");
        params.set("vnp_ResponseCode", code);
        params.set("vnp_TransactionStatus", code);
        params.set("vnp_TransactionNo", "123456");
        params.set(
                "vnp_PayDate",
                DateTimeFormatter.ofPattern("yyyyMMddHHmmss")
                        .withZone(ZoneId.of("Asia/Ho_Chi_Minh"))
                        .format(Instant.now()));
        params.set("vnp_SecureHash", gateway.sign(params.toSingleValueMap()));
        return params;
    }

    private void storedAttempt() {
        when(attempts.findByTxnRef("test-reference")).thenReturn(Optional.of(attempt));
        when(orders.findWithItemsById(7L)).thenReturn(Optional.of(order));
    }

    @Test
    void validIpnMarksPaidOnceAndDuplicateDoesNotOverwrite() {
        storedAttempt();
        assertEquals("00", service.receiveIpn(callback("00")).getRspCode());
        assertEquals(PaymentStatus.PAID, order.getPaymentStatus());
        assertEquals(OrderStatus.PENDING, order.getStatus());
        assertEquals("02", service.receiveIpn(callback("00")).getRspCode());
        assertEquals("02", service.receiveIpn(callback("24")).getRspCode());
        verify(orderService, never()).closeUnpaidPayment(any(), any());
        verify(attempts).flush();
    }

    @Test
    void invalidSignatureDoesNotLookUpOrMutateOrder() {
        var params = callback("00");
        params.set("vnp_Amount", "100");
        assertEquals("97", service.receiveIpn(params).getRspCode());
        verify(attempts, never()).findByTxnRef(any());
        assertEquals(PaymentStatus.PENDING, order.getPaymentStatus());
    }

    @Test
    void suspiciousDebitedPaymentRequiresReviewInsteadOfDiscardingMoney() {
        storedAttempt();
        assertEquals("00", service.receiveIpn(callback("07")).getRspCode());
        assertEquals(PaymentStatus.REVIEW_REQUIRED, order.getPaymentStatus());
        assertEquals(PaymentStatus.REVIEW_REQUIRED, attempt.getStatus());
        verify(orderService).closeUnpaidPayment(7L, PaymentStatus.FAILED);
    }

    @Test
    void signedWrongAmountIsRejectedWithoutMutation() {
        storedAttempt();
        var params = callback("00");
        params.set("vnp_Amount", "100");
        params.set("vnp_SecureHash", gateway.sign(params.toSingleValueMap()));
        assertEquals("04", service.receiveIpn(params).getRspCode());
        assertEquals(PaymentStatus.PENDING, order.getPaymentStatus());
    }

    @Test
    void failureClosesUnpaidOrderAndDuplicateIsAcknowledged() {
        storedAttempt();
        assertEquals("00", service.receiveIpn(callback("24")).getRspCode());
        verify(orderService).closeUnpaidPayment(7L, PaymentStatus.FAILED);
        assertEquals("02", service.receiveIpn(callback("24")).getRspCode());
    }

    @Test
    void paymentAfterCancellationRequiresReviewWithoutReopeningOrder() {
        storedAttempt();
        orderBusinessService.updateStatus(order, OrderStatus.CANCELLED, "CUSTOMER");
        assertEquals("00", service.receiveIpn(callback("00")).getRspCode());
        assertEquals(PaymentStatus.REVIEW_REQUIRED, order.getPaymentStatus());
        assertEquals(OrderStatus.CANCELLED, order.getStatus());
        verify(orderService, never()).closeUnpaidPayment(any(), any());
    }

    @Test
    void successfulResultAfterEarlierFailureIsNotDiscarded() {
        storedAttempt();
        orderBusinessService.updateStatus(order, OrderStatus.CANCELLED, "SYSTEM");
        attempt.finish(PaymentStatus.FAILED, null, "24", null);
        assertEquals("00", service.receiveIpn(callback("00")).getRspCode());
        assertEquals(PaymentStatus.REVIEW_REQUIRED, attempt.getStatus());
    }

    @Test
    void validReturnOnlyRedirectsAndDoesNotMarkPaid() {
        when(attempts.findByTxnRef("test-reference")).thenReturn(Optional.of(attempt));
        assertEquals(
                "https://demo.example.com/orders/7?payment=returned",
                service.returnLocation(callback("00")));
        assertEquals(PaymentStatus.PENDING, order.getPaymentStatus());
        verify(orders, never()).findWithItemsById(any());
    }

    @Test
    void repeatedLinkRequestsReuseTheSameReference() {
        when(orders.findByIdAndUserPhoneNumber(7L, "0900000000")).thenReturn(Optional.of(order));
        when(attempts.findByOrder_Id(7L)).thenReturn(Optional.of(attempt));
        assertEquals(
                attempt.getPaymentUrl(),
                service.createLink(7L, "0900000000", "127.0.0.1").getPaymentUrl());
        verify(attempts, never()).save(any());
    }

    @Test
    void newLinkIsPersistedBeforeReturningAndUsesDatabaseTotal() {
        when(orders.findByIdAndUserPhoneNumber(7L, "0900000000")).thenReturn(Optional.of(order));
        when(attempts.findByOrder_Id(7L)).thenReturn(Optional.empty());
        when(attempts.save(any())).thenAnswer(call -> call.getArgument(0));
        var link = service.createLink(7L, "0900000000", "127.0.0.1");
        org.junit.jupiter.api.Assertions.assertTrue(
                link.getPaymentUrl().contains("vnp_Amount=3000000"));
        verify(attempts).save(any());
    }

    @Test
    void expiredOrderCannotCreateLink() {
        orderBusinessService.selectOnlinePayment(order, Instant.now().minusSeconds(1));
        when(orders.findByIdAndUserPhoneNumber(7L, "0900000000")).thenReturn(Optional.of(order));
        assertThrows(
                IllegalStateException.class,
                () -> service.createLink(7L, "0900000000", "127.0.0.1"));
        verify(attempts, never()).save(any());
    }

    @Test
    void otherUserCannotCreatePaymentLink() {
        assertThrows(
                ResourceNotFoundException.class,
                () -> service.createLink(7L, "other", "127.0.0.1"));
    }

    @Test
    void unpaidOnlineOrderCannotBeConfirmedAndPaidOrderCannotBeCancelled() {
        assertThrows(
                IllegalStateException.class,
                () -> orderBusinessService.updateStatus(order, OrderStatus.CONFIRMED));
        orderBusinessService.recordPayment(order, PaymentStatus.PAID);
        assertThrows(
                IllegalStateException.class,
                () -> orderBusinessService.updateStatus(order, OrderStatus.CANCELLED));
        orderBusinessService.updateStatus(order, OrderStatus.CONFIRMED);
        assertEquals(OrderStatus.CONFIRMED, order.getStatus());
    }
}

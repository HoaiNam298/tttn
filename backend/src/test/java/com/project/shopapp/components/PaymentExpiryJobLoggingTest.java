package com.project.shopapp.components;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.project.shopapp.enums.PaymentStatus;
import com.project.shopapp.repositories.OrderRepository;
import com.project.shopapp.services.OrderService;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

class PaymentExpiryJobLoggingTest {
    private final OrderRepository orders = mock(OrderRepository.class);
    private final OrderService service = mock(OrderService.class);
    private final PaymentExpiryJob job = new PaymentExpiryJob(orders, service);

    @AfterEach
    void cleanup() {
        MDC.clear();
    }

    @Test
    void continuesAfterAnOrderFailureAndCleansJobContext() {
        when(orders.findExpiredPaymentIds(any(), any())).thenReturn(List.of(1L, 2L));
        doAnswer(
                        invocation -> {
                            assertNotNull(MDC.get("jobRunId"));
                            throw new IllegalStateException("failure");
                        })
                .when(service)
                .closeUnpaidPayment(1L, PaymentStatus.EXPIRED);
        job.expire();
        verify(service).closeUnpaidPayment(2L, PaymentStatus.EXPIRED);
        assertNull(MDC.get("jobRunId"));
    }

    @Test
    void restoresContextWhenFetchingBatchFails() {
        MDC.put("jobRunId", "outer-job");
        when(orders.findExpiredPaymentIds(any(), any()))
                .thenThrow(new IllegalStateException("failure"));
        assertDoesNotThrow(job::expire);
        assertEquals("outer-job", MDC.get("jobRunId"));
    }
}

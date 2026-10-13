package com.project.shopapp.components;

import com.project.shopapp.enums.PaymentStatus;
import com.project.shopapp.repositories.OrderRepository;
import com.project.shopapp.services.OrderService;
import java.time.Instant;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.vnpay.expiry-job-enabled", matchIfMissing = true)
@Slf4j
public class PaymentExpiryJob {
    private final OrderRepository orders;
    private final OrderService service;

    public PaymentExpiryJob(OrderRepository orders, OrderService service) {
        this.orders = orders;
        this.service = service;
    }

    @Scheduled(fixedDelay = 60000, initialDelay = 60000)
    public void expire() {
        String previousJobRunId = MDC.get("jobRunId");
        MDC.put("jobRunId", UUID.randomUUID().toString());
        try {
            var ids = orders.findExpiredPaymentIds(Instant.now(), PageRequest.of(0, 100));
            if (!ids.isEmpty()) {
                log.info("Payment expiry batch started selectedOrders={}", ids.size());
            }
            for (Long id : ids) {
                try {
                    service.closeUnpaidPayment(id, PaymentStatus.EXPIRED);
                } catch (RuntimeException exception) {
                    log.error("Cannot expire payment orderId={}", id, exception);
                }
            }
            log.debug("Payment expiry batch finished selectedOrders={}", ids.size());
        } catch (RuntimeException exception) {
            log.error("Payment expiry batch failed", exception);
        } finally {
            if (previousJobRunId == null) {
                MDC.remove("jobRunId");
            } else {
                MDC.put("jobRunId", previousJobRunId);
            }
        }
    }
}

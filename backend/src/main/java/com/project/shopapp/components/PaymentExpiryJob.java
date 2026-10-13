package com.project.shopapp.components;

import com.project.shopapp.enums.PaymentStatus;
import com.project.shopapp.repositories.OrderRepository;
import com.project.shopapp.services.OrderService;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.vnpay.expiry-job-enabled", matchIfMissing = true)
public class PaymentExpiryJob {
    private static final Logger LOGGER = LoggerFactory.getLogger(PaymentExpiryJob.class);
    private final OrderRepository orders;
    private final OrderService service;

    public PaymentExpiryJob(OrderRepository orders, OrderService service) {
        this.orders = orders;
        this.service = service;
    }

    @Scheduled(fixedDelay = 60000, initialDelay = 60000)
    public void expire() {
        for (Long id : orders.findExpiredPaymentIds(Instant.now(), PageRequest.of(0, 100))) {
            try {
                service.closeUnpaidPayment(id, PaymentStatus.EXPIRED);
            } catch (RuntimeException exception) {
                LOGGER.error("Cannot expire payment for order {}", id, exception);
            }
        }
    }
}

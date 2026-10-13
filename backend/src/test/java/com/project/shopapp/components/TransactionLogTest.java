package com.project.shopapp.components;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.MDC;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

class TransactionLogTest {
    private final Logger logger = mock(Logger.class);
    private final TransactionTemplate transaction =
            new TransactionTemplate(new TestTransactionManager());

    @AfterEach
    void cleanup() {
        MDC.clear();
    }

    @Test
    void logsOnlyAfterCommitWithOriginalContextAndRestoresCurrentContext() {
        doAnswer(
                        invocation -> {
                            assertEquals("request-original", MDC.get("requestId"));
                            assertEquals("job-original", MDC.get("jobRunId"));
                            return null;
                        })
                .when(logger)
                .info(eq("Order created orderId={}"), any(Object[].class));
        transaction.executeWithoutResult(
                status -> {
                    MDC.put("requestId", "request-original");
                    MDC.put("jobRunId", "job-original");
                    TransactionLog.info(logger, "Order created orderId={}", 105L);
                    verifyNoInteractions(logger);
                    MDC.put("requestId", "request-current");
                    MDC.remove("jobRunId");
                });
        verify(logger).info("Order created orderId={}", new Object[] {105L});
        assertEquals("request-current", MDC.get("requestId"));
        assertNull(MDC.get("jobRunId"));
    }

    @Test
    void rollbackDoesNotProduceSuccessLog() {
        transaction.executeWithoutResult(
                status -> {
                    TransactionLog.info(logger, "Order created orderId={}", 105L);
                    status.setRollbackOnly();
                });
        verifyNoInteractions(logger);
    }

    @Test
    void logsImmediatelyWithoutTransaction() {
        TransactionLog.info(logger, "File stored fileId={}", "generated-id");
        verify(logger).info("File stored fileId={}", new Object[] {"generated-id"});
    }

    private static class TestTransactionManager extends AbstractPlatformTransactionManager {
        @Override
        protected Object doGetTransaction() {
            return new Object();
        }

        @Override
        protected void doBegin(Object transaction, TransactionDefinition definition) {}

        @Override
        protected void doCommit(DefaultTransactionStatus status) {}

        @Override
        protected void doRollback(DefaultTransactionStatus status) {}
    }
}

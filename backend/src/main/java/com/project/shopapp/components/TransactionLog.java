package com.project.shopapp.components;

import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.MDC;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

public final class TransactionLog {
    private TransactionLog() {}

    public static void info(Logger logger, String message, Object... arguments) {
        afterCommit(() -> logger.info(message, arguments));
    }

    public static void warn(Logger logger, String message, Object... arguments) {
        afterCommit(() -> logger.warn(message, arguments));
    }

    public static void debug(Logger logger, String message, Object... arguments) {
        afterCommit(() -> logger.debug(message, arguments));
    }

    private static void afterCommit(Runnable action) {
        Map<String, String> context = MDC.getCopyOfContextMap();
        Runnable contextualAction =
                () -> {
                    Map<String, String> previous = MDC.getCopyOfContextMap();
                    try {
                        restore(context);
                        action.run();
                    } finally {
                        restore(previous);
                    }
                };
        if (TransactionSynchronizationManager.isActualTransactionActive()
                && TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(
                    new TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            contextualAction.run();
                        }
                    });
        } else {
            contextualAction.run();
        }
    }

    private static void restore(Map<String, String> context) {
        if (context == null) {
            MDC.clear();
        } else {
            MDC.setContextMap(context);
        }
    }
}

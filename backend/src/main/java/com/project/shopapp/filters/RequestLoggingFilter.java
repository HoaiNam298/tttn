package com.project.shopapp.filters;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@Slf4j
public class RequestLoggingFilter extends OncePerRequestFilter {
    public static final String REQUEST_ID_HEADER = "X-Request-ID";
    private static final String REQUEST_ID = "requestId";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Map<String, String> previousContext = MDC.getCopyOfContextMap();
        MDC.put(REQUEST_ID, UUID.randomUUID().toString());
        response.setHeader(REQUEST_ID_HEADER, MDC.get(REQUEST_ID));
        long started = System.nanoTime();
        boolean completed = false;
        try {
            chain.doFilter(request, response);
            completed = true;
        } finally {
            try {
                long durationMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started);
                String path = request.getRequestURI();
                if (!completed) {
                    log.warn(
                            "HTTP request aborted method={} path={} durationMs={}",
                            request.getMethod(),
                            path,
                            durationMs);
                } else if (isHealthCheck(path)) {
                    log.debug(
                            "HTTP request completed method={} path={} status={} durationMs={}",
                            request.getMethod(),
                            path,
                            response.getStatus(),
                            durationMs);
                } else if (response.getStatus() >= 400) {
                    log.warn(
                            "HTTP request completed method={} path={} status={} durationMs={}",
                            request.getMethod(),
                            path,
                            response.getStatus(),
                            durationMs);
                } else {
                    log.info(
                            "HTTP request completed method={} path={} status={} durationMs={}",
                            request.getMethod(),
                            path,
                            response.getStatus(),
                            durationMs);
                }
            } finally {
                if (previousContext == null) {
                    MDC.clear();
                } else {
                    MDC.setContextMap(previousContext);
                }
            }
        }
    }

    private boolean isHealthCheck(String path) {
        return path.equals("/api/v1/health")
                || path.equals("/actuator/health")
                || path.startsWith("/actuator/health/");
    }
}

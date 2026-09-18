package com.icici.leavemanagement.common.config;

import java.io.IOException;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class RequestLoggingFilter extends OncePerRequestFilter {
    public static final String REQUEST_ID_HEADER = "X-Request-Id";
    private static final String REQUEST_ID_MDC_KEY = "requestId";
    private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        long start = System.nanoTime();
        String requestId = request.getHeader(REQUEST_ID_HEADER);
        if (requestId == null || requestId.isBlank()) {
            requestId = UUID.randomUUID().toString();
        }
        response.setHeader(REQUEST_ID_HEADER, requestId);
        MDC.put(REQUEST_ID_MDC_KEY, requestId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            long durationMs = (System.nanoTime() - start) / 1_000_000;
            String query = request.getQueryString() == null ? "" : "?" + request.getQueryString();
            String user = request.getUserPrincipal() == null ? "anonymous" : request.getUserPrincipal().getName();
            int status = response.getStatus();
            String message = "requestId={} user={} {} {}{} -> {} ({} ms)";
            if (status >= 500) {
                log.error(message, requestId, user, request.getMethod(), request.getRequestURI(), query, status, durationMs);
            } else if (status >= 400) {
                log.warn(message, requestId, user, request.getMethod(), request.getRequestURI(), query, status, durationMs);
            } else {
                log.info(message, requestId, user, request.getMethod(), request.getRequestURI(), query, status, durationMs);
            }
            MDC.remove(REQUEST_ID_MDC_KEY);
        }
    }
}

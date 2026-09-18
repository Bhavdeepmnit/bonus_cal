package com.icici.leavemanagement.common.security;

import java.io.IOException;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import com.icici.leavemanagement.common.exception.ErrorBody;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import tools.jackson.databind.json.JsonMapper;

/** Writes 401 (not logged in) and 403 (wrong role) in the same JSON format as GlobalExceptionHandler. */
@Component
@RequiredArgsConstructor
public class JsonSecurityErrorHandler implements AuthenticationEntryPoint, AccessDeniedHandler {

    private static final Logger log = LoggerFactory.getLogger(JsonSecurityErrorHandler.class);

    private final JsonMapper jsonMapper;

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
            AuthenticationException ex) throws IOException {
        log.warn("Authentication failed method={} uri={} remoteAddress={}", request.getMethod(),
            request.getRequestURI(), request.getRemoteAddr());
        response.setHeader("WWW-Authenticate", "Basic realm=\"leave-management\"");
        write(response, HttpStatus.UNAUTHORIZED,
            "Login required: send a valid email and password (HTTP Basic auth). Deactivated accounts cannot log in.");
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
            AccessDeniedException ex) throws IOException {
        log.warn("Access denied method={} uri={} user={}", request.getMethod(), request.getRequestURI(),
            request.getUserPrincipal() == null ? "anonymous" : request.getUserPrincipal().getName());
        write(response, HttpStatus.FORBIDDEN, "Your role is not allowed to do this");
    }

    private void write(HttpServletResponse response, HttpStatus status, String message) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        jsonMapper.writeValue(response.getOutputStream(), ErrorBody.of(status, message));
    }
}

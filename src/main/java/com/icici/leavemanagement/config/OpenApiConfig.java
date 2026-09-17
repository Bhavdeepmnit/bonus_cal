package com.icici.leavemanagement.config;

import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;

/** Swagger UI at /swagger-ui.html. Click "Authorize" and enter an employee email + password. */
@Configuration
@SecurityScheme(name = "basicAuth", type = SecuritySchemeType.HTTP, scheme = "basic")
@OpenAPIDefinition(
    info = @Info(title = "Leave Management API", version = "v1",
        description = "Employees, leave policies, holidays, leave requests, approvals and notifications"),
    security = @SecurityRequirement(name = "basicAuth"))
public class OpenApiConfig {
}

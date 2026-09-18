package com.icici.leavemanagement.common.config;

import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Swagger UI at /swagger-ui.html. Click "Authorize" and enter an employee email + password.
 * The OpenAPI file (/v3/api-docs or /v3/api-docs.yaml) can be imported into Postman.
 * Tags are listed here so they appear in this order (Postman folders, Swagger sections).
 */
@Configuration
@SecurityScheme(name = "basicAuth", type = SecuritySchemeType.HTTP, scheme = "basic",
    description = "Username = employee email, password = employee password")
@OpenAPIDefinition(
    info = @Info(title = "Leave Management API", version = "v1",
        description = "Employees, leave policies, holidays, leave requests, approvals and notifications. "
            + "All /api endpoints need HTTP Basic auth (employee email + password)."),
    servers = @Server(url = "http://localhost:8080", description = "Local"),
    security = @SecurityRequirement(name = "basicAuth"),
    tags = {
        @Tag(name = "1. Employees", description = "Accounts, roles and reporting lines"),
        @Tag(name = "2. Leave policies", description = "Yearly quota and carry-forward per grade and leave type"),
        @Tag(name = "3. Holidays", description = "Company holidays (not counted as leave days)"),
        @Tag(name = "4. Leave requests", description = "Apply, view, cancel, balance and yearly summary"),
        @Tag(name = "5. Approvals", description = "Manager decisions (tasks are created automatically)"),
        @Tag(name = "6. Notifications", description = "Notifications of the logged-in user")
    })
public class OpenApiConfig {
}

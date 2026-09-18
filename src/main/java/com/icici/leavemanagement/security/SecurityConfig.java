package com.icici.leavemanagement.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * HTTP Basic login with the employee's email and password.
 * Roles: EMPLOYEE (apply for leave), MANAGER (also approve), HR (also manage employees, policies, holidays).
 * Rules on *which* record a user may see (own / team / all) are checked in the services.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, JsonSecurityErrorHandler errors) throws Exception {
        http
            .csrf(csrf -> csrf.disable())   // stateless REST API, no browser session cookies
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .httpBasic(basic -> basic.authenticationEntryPoint(errors))
            .exceptionHandling(e -> e.authenticationEntryPoint(errors).accessDeniedHandler(errors))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/health", "/actuator/info", "/error",
                    "/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs", "/v3/api-docs.yaml", "/v3/api-docs/**").permitAll()

                .requestMatchers(HttpMethod.GET, "/api/employees/me").authenticated()
                .requestMatchers(HttpMethod.PUT, "/api/employees/me/password").authenticated()
                .requestMatchers(HttpMethod.GET, "/api/employees").hasAnyRole("HR", "MANAGER")
                .requestMatchers(HttpMethod.GET, "/api/employees/*").authenticated()
                .requestMatchers("/api/employees/**").hasRole("HR")

                .requestMatchers(HttpMethod.POST, "/api/policies/evaluate").authenticated()
                .requestMatchers(HttpMethod.GET, "/api/policies/**", "/api/holidays/**").authenticated()
                .requestMatchers("/api/policies/**", "/api/holidays/**").hasRole("HR")

                .requestMatchers("/api/approvals/**").hasAnyRole("MANAGER", "HR")

                .anyRequest().authenticated());
        return http.build();
    }

    @Bean
    PasswordEncoder passwordEncoder(@Value("${app.security.bcrypt-strength:10}") int strength) {
        return new BCryptPasswordEncoder(strength);
    }
}

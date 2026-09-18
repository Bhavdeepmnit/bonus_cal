package com.icici.leavemanagement.employee;

import java.time.LocalDate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

/** Creates the first HR account (app.admin.email / app.admin.password) when no HR user exists. */
@Component
@RequiredArgsConstructor
public class AdminSeeder implements CommandLineRunner {
    private static final Logger log = LoggerFactory.getLogger(AdminSeeder.class);

    private final EmployeeRepository repository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.email}")
    private String adminEmail;

    @Value("${app.admin.password}")
    private String adminPassword;

    @Override
    public void run(String... args) {
        if (repository.existsByRoleAndActiveTrue(Role.HR)) {
            return;
        }
        Employee admin = new Employee();
        admin.setName("HR Admin");
        admin.setEmail(adminEmail.toLowerCase());
        admin.setPasswordHash(passwordEncoder.encode(adminPassword));
        admin.setGrade("L3");
        admin.setRole(Role.HR);
        admin.setJoiningDate(LocalDate.now());
        repository.save(admin);
        log.warn("Created HR admin account '{}'. Log in with it and change the password (PUT /api/employees/me/password).", adminEmail);
    }
}

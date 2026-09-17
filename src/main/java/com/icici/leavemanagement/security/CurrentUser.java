package com.icici.leavemanagement.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import com.icici.leavemanagement.employee.Employee;
import com.icici.leavemanagement.employee.EmployeeRepository;
import com.icici.leavemanagement.exception.NotAllowedException;

import lombok.RequiredArgsConstructor;

/** Gives services the Employee who is logged in for the current request. */
@Component
@RequiredArgsConstructor
public class CurrentUser {

    private final EmployeeRepository repository;

    public Employee get() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new NotAllowedException("Login required");
        }
        return repository.findByEmailIgnoreCase(auth.getName())
            .orElseThrow(() -> new NotAllowedException("The logged-in user no longer exists"));
    }
}

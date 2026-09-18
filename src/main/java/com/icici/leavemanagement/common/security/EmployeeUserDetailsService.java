package com.icici.leavemanagement.common.security;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.icici.leavemanagement.employee.Employee;
import com.icici.leavemanagement.employee.EmployeeRepository;

import lombok.RequiredArgsConstructor;

/** Login = employee email. Deactivated employees cannot log in. */
@Service
@RequiredArgsConstructor
public class EmployeeUserDetailsService implements UserDetailsService {

    private final EmployeeRepository repository;

    @Override
    public UserDetails loadUserByUsername(String email) {
        Employee e = repository.findByEmailIgnoreCase(email)
            .orElseThrow(() -> new UsernameNotFoundException("No employee with email " + email));
        return User.withUsername(e.getEmail())
            .password(e.getPasswordHash())
            .roles(e.getRole().name())
            .disabled(!e.isActive())
            .build();
    }
}

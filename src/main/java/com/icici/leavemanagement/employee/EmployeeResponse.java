package com.icici.leavemanagement.employee;

import java.time.LocalDate;

/** What the API returns for an employee (no password hash). */
public record EmployeeResponse(Long id, String name, String email, String grade, Role role,
        Long managerId, LocalDate joiningDate, boolean active) {

    public static EmployeeResponse from(Employee e) {
        return new EmployeeResponse(e.getId(), e.getName(), e.getEmail(), e.getGrade(), e.getRole(),
            e.getManagerId(), e.getJoiningDate(), e.isActive());
    }
}

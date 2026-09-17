package com.icici.leavemanagement.employee;

import java.time.LocalDate;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "employees")
@Getter @Setter @NoArgsConstructor
public class Employee {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private String email;          // also the login name
    private String passwordHash;   // BCrypt, never returned by the API
    private String grade;          // L1, L2, L3
    @Enumerated(EnumType.STRING)
    private Role role;
    private Long managerId;        // approves leave for this employee (null for top-level)
    private LocalDate joiningDate;
    private boolean active = true; // false = deactivated (kept for leave history)
    @Version
    private Long version;

    public boolean isHr() {
        return role == Role.HR;
    }

    /** Own record, a direct report, or anyone if HR. */
    public boolean canView(Employee other) {
        return id.equals(other.getId()) || id.equals(other.getManagerId()) || isHr();
    }
}

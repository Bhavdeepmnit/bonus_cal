package com.icici.leavemanagement.employee;

import java.time.LocalDate;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @NoArgsConstructor @AllArgsConstructor
public class CreateEmployeeRequest {
    @NotBlank(message = "name is required")
    @Size(max = 100, message = "name must be at most 100 characters")
    private String name;

    @NotBlank(message = "email is required")
    @Email(message = "email must be a valid email address")
    @Size(max = 150, message = "email must be at most 150 characters")
    private String email;

    @NotBlank(message = "password is required")
    @Size(min = 8, max = 72, message = "password must be 8 to 72 characters")
    private String password;

    @NotBlank(message = "grade is required")
    @Pattern(regexp = "L1|L2|L3", message = "grade must be L1, L2 or L3")
    private String grade;

    @NotBlank(message = "role is required")
    @Pattern(regexp = Role.PATTERN, message = "role must be EMPLOYEE, MANAGER or HR")
    private String role;

    private Long managerId;

    /** Optional, defaults to today. Used for carry-forward (no carry-forward in the joining year). */
    private LocalDate joiningDate;
}

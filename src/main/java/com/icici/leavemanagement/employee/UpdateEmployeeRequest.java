package com.icici.leavemanagement.employee;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** PUT body: replaces name, grade, role and manager. Email (the login) cannot be changed. */
@Data @NoArgsConstructor @AllArgsConstructor
public class UpdateEmployeeRequest {
    @NotBlank(message = "name is required")
    @Size(max = 100, message = "name must be at most 100 characters")
    private String name;

    @NotBlank(message = "grade is required")
    @Pattern(regexp = "L1|L2|L3", message = "grade must be L1, L2 or L3")
    private String grade;

    @NotBlank(message = "role is required")
    @Pattern(regexp = Role.PATTERN, message = "role must be EMPLOYEE, MANAGER or HR")
    private String role;

    private Long managerId;
}

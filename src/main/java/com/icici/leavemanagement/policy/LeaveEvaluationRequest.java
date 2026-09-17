package com.icici.leavemanagement.policy;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @AllArgsConstructor @NoArgsConstructor
public class LeaveEvaluationRequest {
    @NotBlank(message = "grade is required")
    private String grade;

    @NotBlank(message = "leaveType is required")
    @Pattern(regexp = LeaveType.PATTERN, message = "leaveType must be CASUAL, SICK or EARNED")
    private String leaveType;

    @NotNull(message = "requestedDays is required")
    @Min(value = 1, message = "requestedDays must be at least 1")
    private Integer requestedDays;

    @NotNull(message = "alreadyUsedDays is required")
    @Min(value = 0, message = "alreadyUsedDays must be 0 or more")
    private Integer alreadyUsedDays;

    /** Optional: days carried over from last year (0 if left out). */
    @Min(value = 0, message = "carriedForwardDays must be 0 or more")
    private Integer carriedForwardDays;
}

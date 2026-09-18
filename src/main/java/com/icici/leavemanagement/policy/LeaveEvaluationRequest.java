package com.icici.leavemanagement.policy;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @AllArgsConstructor @NoArgsConstructor
public class LeaveEvaluationRequest {
    @Schema(example = "L1")
    @NotBlank(message = "grade is required")
    private String grade;

    @Schema(example = "CASUAL")
    @NotBlank(message = "leaveType is required")
    @Pattern(regexp = LeaveType.PATTERN, message = "leaveType must be CASUAL, SICK or EARNED")
    private String leaveType;

    @Schema(example = "12")
    @NotNull(message = "requestedDays is required")
    @Min(value = 1, message = "requestedDays must be at least 1")
    private Integer requestedDays;

    @Schema(example = "0")
    @NotNull(message = "alreadyUsedDays is required")
    @Min(value = 0, message = "alreadyUsedDays must be 0 or more")
    private Integer alreadyUsedDays;

    /** Optional: days carried over from last year (0 if left out). */
    @Schema(example = "3")
    @Min(value = 0, message = "carriedForwardDays must be 0 or more")
    private Integer carriedForwardDays;
}

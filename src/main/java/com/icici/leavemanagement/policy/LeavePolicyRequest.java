package com.icici.leavemanagement.policy;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @NoArgsConstructor @AllArgsConstructor
public class LeavePolicyRequest {
    @Schema(example = "EARNED")
    @NotBlank(message = "leaveType is required")
    @Pattern(regexp = LeaveType.PATTERN, message = "leaveType must be CASUAL, SICK or EARNED")
    private String leaveType;

    @Schema(example = "L3")
    @NotBlank(message = "grade is required")
    @Pattern(regexp = "L1|L2|L3", message = "grade must be L1, L2 or L3")
    private String grade;

    @Schema(example = "21")
    @NotNull(message = "annualQuota is required")
    @Min(value = 0, message = "annualQuota must be 0 or more")
    @Max(value = 365, message = "annualQuota must be at most 365")
    private Integer annualQuota;

    @Schema(example = "8")
    @NotNull(message = "carryForwardLimit is required")
    @Min(value = 0, message = "carryForwardLimit must be 0 or more")
    @Max(value = 365, message = "carryForwardLimit must be at most 365")
    private Integer carryForwardLimit;
}

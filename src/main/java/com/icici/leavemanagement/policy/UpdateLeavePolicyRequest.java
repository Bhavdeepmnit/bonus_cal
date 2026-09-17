package com.icici.leavemanagement.policy;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** PUT body: grade and leave type identify the policy and cannot change. */
@Data @NoArgsConstructor @AllArgsConstructor
public class UpdateLeavePolicyRequest {
    @NotNull(message = "annualQuota is required")
    @Min(value = 0, message = "annualQuota must be 0 or more")
    @Max(value = 365, message = "annualQuota must be at most 365")
    private Integer annualQuota;

    @NotNull(message = "carryForwardLimit is required")
    @Min(value = 0, message = "carryForwardLimit must be 0 or more")
    @Max(value = 365, message = "carryForwardLimit must be at most 365")
    private Integer carryForwardLimit;
}

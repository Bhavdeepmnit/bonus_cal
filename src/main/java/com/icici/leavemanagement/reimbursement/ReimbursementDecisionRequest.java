package com.icici.leavemanagement.reimbursement;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @NoArgsConstructor @AllArgsConstructor
public class ReimbursementDecisionRequest {
    @Schema(example = "APPROVED")
    @NotBlank(message = "status is required")
    @Pattern(regexp = "APPROVED|REJECTED", message = "status must be APPROVED or REJECTED")
    private String status;
}

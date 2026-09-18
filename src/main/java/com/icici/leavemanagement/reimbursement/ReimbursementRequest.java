package com.icici.leavemanagement.reimbursement;

import java.math.BigDecimal;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @NoArgsConstructor @AllArgsConstructor
public class ReimbursementRequest {
    @Schema(example = "TRAIN")
    @NotBlank(message = "travelType is required")
    @Size(max = 50, message = "travelType must be at most 50 characters")
    private String travelType;

    @Schema(example = "Jaipur")
    @NotBlank(message = "source is required")
    @Size(max = 100, message = "source must be at most 100 characters")
    private String source;

    @Schema(example = "Delhi")
    @NotBlank(message = "destination is required")
    @Size(max = 100, message = "destination must be at most 100 characters")
    private String destination;

    @Schema(example = "1250.50")
    @NotNull(message = "amount is required")
    @DecimalMin(value = "0.01", message = "amount must be greater than 0")
    private BigDecimal amount;
}

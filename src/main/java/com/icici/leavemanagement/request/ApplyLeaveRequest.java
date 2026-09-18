package com.icici.leavemanagement.request;

import java.time.LocalDate;

import com.icici.leavemanagement.policy.LeaveType;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** The employee is the logged-in user, so there is no employeeId here. */
@Data @NoArgsConstructor @AllArgsConstructor
public class ApplyLeaveRequest {
    @Schema(example = "CASUAL")
    @NotBlank(message = "leaveType is required")
    @Pattern(regexp = LeaveType.PATTERN, message = "leaveType must be CASUAL, SICK or EARNED")
    private String leaveType;

    @Schema(example = "2026-09-28")
    @NotNull(message = "startDate is required (yyyy-MM-dd)")
    private LocalDate startDate;

    @Schema(example = "2026-10-02")
    @NotNull(message = "endDate is required (yyyy-MM-dd)")
    private LocalDate endDate;

    @Schema(example = "Family function")
    @Size(max = 255, message = "reason must be at most 255 characters")
    private String reason;
}

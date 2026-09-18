package com.icici.leavemanagement.employee;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @NoArgsConstructor @AllArgsConstructor
public class ChangePasswordRequest {
    @Schema(example = "Ravi@1234")
    @NotBlank(message = "currentPassword is required")
    private String currentPassword;

    @Schema(example = "Ravi@5678")
    @NotBlank(message = "newPassword is required")
    @Size(min = 8, max = 72, message = "newPassword must be 8 to 72 characters")
    private String newPassword;
}

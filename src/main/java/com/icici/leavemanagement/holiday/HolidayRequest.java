package com.icici.leavemanagement.holiday;

import java.time.LocalDate;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @NoArgsConstructor @AllArgsConstructor
public class HolidayRequest {
    @Schema(example = "2026-10-02")
    @NotNull(message = "date is required (yyyy-MM-dd)")
    private LocalDate date;

    @Schema(example = "Gandhi Jayanti")
    @NotBlank(message = "name is required")
    @Size(max = 100, message = "name must be at most 100 characters")
    private String name;
}

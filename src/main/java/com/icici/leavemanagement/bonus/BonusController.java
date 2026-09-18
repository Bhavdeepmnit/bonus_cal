package com.icici.leavemanagement.bonus;

import java.time.LocalDate;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "8. Bonuses")
@RestController
@RequestMapping("/api/bonuses")
@RequiredArgsConstructor
public class BonusController {
    private final BonusService service;

    @Operation(summary = "Calculate an employee's yearly bonus from leave and reimbursement activity")
    @GetMapping("/{employeeId}")
    public BonusResponse calculate(@Parameter(example = "3") @PathVariable Long employeeId,
                                   @Parameter(example = "2026") @RequestParam(required = false) Integer year) {
        return service.calculate(employeeId, year != null ? year : LocalDate.now().getYear());
    }
}
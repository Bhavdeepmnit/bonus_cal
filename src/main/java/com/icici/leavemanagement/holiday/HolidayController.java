package com.icici.leavemanagement.holiday;

import java.time.LocalDate;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "3. Holidays")
@RestController
@RequestMapping("/api/holidays")
@RequiredArgsConstructor
public class HolidayController {
    private final HolidayService service;

    // GET /api/holidays?year=2026  (year defaults to the current year)
    @Operation(summary = "List holidays of a year")
    @GetMapping
    public List<Holiday> getByYear(@Parameter(example = "2026") @RequestParam(required = false) Integer year) {
        return service.getByYear(year != null ? year : LocalDate.now().getYear());
    }

    // POST /api/holidays  (HR) -> 201 Created
    @Operation(summary = "Add holiday (HR)")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Holiday create(@Valid @RequestBody HolidayRequest request) { return service.create(request); }

    // DELETE /api/holidays/1  (HR) -> 204
    @Operation(summary = "Delete holiday (HR)")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@Parameter(example = "2") @PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}

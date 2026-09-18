package com.icici.leavemanagement.employee;

import java.net.URI;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "1. Employees")
@RestController
@RequestMapping("/api/employees")
@RequiredArgsConstructor
public class EmployeeController {
    private final EmployeeService service;

    // POST /api/employees  (HR)  -> 201 Created
    @Operation(summary = "Create employee (HR)")
    @PostMapping
    public ResponseEntity<EmployeeResponse> create(@Valid @RequestBody CreateEmployeeRequest request) {
        EmployeeResponse created = service.create(request);
        return ResponseEntity.created(URI.create("/api/employees/" + created.id())).body(created);
    }

    // GET /api/employees?page=0&size=20&sort=name  (HR: everyone, MANAGER: direct reports)
    @Operation(summary = "List employees (HR: all, MANAGER: own team)")
    @GetMapping
    public PagedModel<EmployeeResponse> list(
            @ParameterObject @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.ASC) Pageable pageable) {
        return new PagedModel<>(service.list(pageable));
    }

    // GET /api/employees/me
    @Operation(summary = "My profile")
    @GetMapping("/me")
    public EmployeeResponse me() { return service.me(); }

    // PUT /api/employees/me/password  -> 204
    @Operation(summary = "Change my password")
    @PutMapping("/me/password")
    public ResponseEntity<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        service.changePassword(request);
        return ResponseEntity.noContent().build();
    }

    // GET /api/employees/1  (self, own manager, or HR)
    @Operation(summary = "Get employee (self, manager or HR)")
    @GetMapping("/{id}")
    public EmployeeResponse getById(@Parameter(example = "3") @PathVariable Long id) { return service.getVisible(id); }

    // PUT /api/employees/1  (HR)
    @Operation(summary = "Update employee (HR)")
    @PutMapping("/{id}")
    public EmployeeResponse update(@Parameter(example = "3") @PathVariable Long id,
                                   @Valid @RequestBody UpdateEmployeeRequest request) {
        return service.update(id, request);
    }

    // DELETE /api/employees/1  (HR) -> 204, deactivates the account
    @Operation(summary = "Deactivate employee (HR)")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deactivate(@Parameter(example = "4") @PathVariable Long id) {
        service.deactivate(id);
        return ResponseEntity.noContent().build();
    }
}

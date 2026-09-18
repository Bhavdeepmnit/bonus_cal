package com.icici.leavemanagement.reimbursement;

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

@Tag(name = "7. Reimbursements")
@RestController
@RequestMapping("/api/reimbursements")
@RequiredArgsConstructor
public class ReimbursementController {
    private final ReimbursementService service;

    @Operation(summary = "Create reimbursement request")
    @PostMapping
    public ResponseEntity<Reimbursement> create(@Valid @RequestBody ReimbursementRequest request) {
        Reimbursement created = service.create(request);
        return ResponseEntity.created(URI.create("/api/reimbursements/" + created.getId())).body(created);
    }

    @Operation(summary = "List visible reimbursement requests")
    @GetMapping
    public PagedModel<Reimbursement> list(
            @ParameterObject @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
        return new PagedModel<>(service.list(pageable));
    }

    @Operation(summary = "Get reimbursement request")
    @GetMapping("/{id}")
    public Reimbursement getById(@Parameter(example = "1") @PathVariable Long id) {
        return service.getVisible(id);
    }

    @Operation(summary = "Update pending reimbursement request")
    @PutMapping("/{id}")
    public Reimbursement update(@Parameter(example = "1") @PathVariable Long id,
                                @Valid @RequestBody ReimbursementRequest request) {
        return service.update(id, request);
    }

    @Operation(summary = "Approve or reject reimbursement request (HR)")
    @PutMapping("/{id}/decision")
    public Reimbursement decide(@Parameter(example = "1") @PathVariable Long id,
                                @Valid @RequestBody ReimbursementDecisionRequest request) {
        return service.decide(id, request);
    }

    @Operation(summary = "Delete pending reimbursement request")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@Parameter(example = "1") @PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}

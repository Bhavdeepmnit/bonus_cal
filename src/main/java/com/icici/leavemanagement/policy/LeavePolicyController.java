package com.icici.leavemanagement.policy;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/policies")
@RequiredArgsConstructor
public class LeavePolicyController {
    private final LeavePolicyService service;

    // GET /api/policies
    @GetMapping
    public List<LeavePolicy> getAll() { return service.getAll(); }

    // POST /api/policies  (HR) -> 201 Created
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public LeavePolicy create(@Valid @RequestBody LeavePolicyRequest request) { return service.create(request); }

    // PUT /api/policies/1  (HR)
    @PutMapping("/{id}")
    public LeavePolicy update(@PathVariable Long id, @Valid @RequestBody UpdateLeavePolicyRequest request) {
        return service.update(id, request);
    }

    // DELETE /api/policies/1  (HR) -> 204
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    // POST /api/policies/evaluate  -> checks numbers against the quota without saving anything
    @PostMapping("/evaluate")
    public LeaveEvaluationResponse evaluate(@Valid @RequestBody LeaveEvaluationRequest request) {
        return service.evaluate(request);
    }
}

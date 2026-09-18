package com.example.reimbursementservice.controller;

import com.example.reimbursementservice.entity.Reimbursement;
import com.example.reimbursementservice.service.ReimbursementService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reimbursements")
public class ReimbursementController {

    private final ReimbursementService reimbursementService;

    public ReimbursementController(ReimbursementService reimbursementService) {
        this.reimbursementService = reimbursementService;
    }

    // CREATE
    @PostMapping
    public Reimbursement createReimbursement(
            @RequestBody Reimbursement reimbursement) {

        return reimbursementService.createReimbursement(reimbursement);
    }

    // READ - Get all
    @GetMapping
    public List<Reimbursement> getAllReimbursements() {

        return reimbursementService.getAllReimbursements();
    }

    // READ - Get by ID
    @GetMapping("/{id}")
public Reimbursement getReimbursementById(
        @PathVariable Long id) {

    return reimbursementService.getReimbursementById(id);
}
    // UPDATE
    @PutMapping("/{id}")
    public Reimbursement updateReimbursement(
            @PathVariable Long id,
            @RequestBody Reimbursement reimbursement) {

        return reimbursementService.updateReimbursement(id, reimbursement);
    }

    // DELETE
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteReimbursement(
            @PathVariable Long id) {

        reimbursementService.deleteReimbursement(id);

        return ResponseEntity.noContent().build();
    }
}
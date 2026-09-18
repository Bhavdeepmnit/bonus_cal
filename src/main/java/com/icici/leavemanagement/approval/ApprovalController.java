package com.icici.leavemanagement.approval;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/** MANAGER / HR only. Tasks are created automatically when a leave request is PENDING. */
@Tag(name = "5. Approvals")
@RestController
@RequestMapping("/api/approvals")
@RequiredArgsConstructor
public class ApprovalController {
    private final ApprovalService service;

    // GET /api/approvals/pending  -> tasks waiting for the logged-in manager
    @Operation(summary = "My pending approvals")
    @GetMapping("/pending")
    public List<ApprovalTask> getMyPending() { return service.getMyPending(); }

    // GET /api/approvals/1
    @Operation(summary = "Get approval task (approver or HR)")
    @GetMapping("/{id}")
    public ApprovalTask getById(@Parameter(example = "1") @PathVariable Long id) { return service.getVisible(id); }

    // PUT /api/approvals/1/decide?decision=APPROVED&comments=ok
    @Operation(summary = "Approve or reject (assigned approver only)")
    @PutMapping("/{id}/decide")
    public ApprovalTask decide(@Parameter(example = "1") @PathVariable Long id,
                               @Parameter(example = "APPROVED", schema = @Schema(allowableValues = { "APPROVED", "REJECTED" }))
                               @RequestParam String decision,
                               @Parameter(example = "Enjoy your time") @RequestParam(required = false) String comments) {
        return service.decide(id, decision, comments);
    }
}

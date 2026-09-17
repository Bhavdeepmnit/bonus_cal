package com.icici.leavemanagement.approval;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

/** MANAGER / HR only. Tasks are created automatically when a leave request is PENDING. */
@RestController
@RequestMapping("/api/approvals")
@RequiredArgsConstructor
public class ApprovalController {
    private final ApprovalService service;

    // GET /api/approvals/pending  -> tasks waiting for the logged-in manager
    @GetMapping("/pending")
    public List<ApprovalTask> getMyPending() { return service.getMyPending(); }

    // GET /api/approvals/1
    @GetMapping("/{id}")
    public ApprovalTask getById(@PathVariable Long id) { return service.getVisible(id); }

    // PUT /api/approvals/1/decide?decision=APPROVED&comments=ok
    @PutMapping("/{id}/decide")
    public ApprovalTask decide(@PathVariable Long id,
                               @RequestParam String decision,
                               @RequestParam(required = false) String comments) {
        return service.decide(id, decision, comments);
    }
}

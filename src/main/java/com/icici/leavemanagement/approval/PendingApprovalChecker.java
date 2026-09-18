package com.icici.leavemanagement.approval;

import org.springframework.stereotype.Component;

import com.icici.leavemanagement.common.port.PendingWorkChecker;

import lombok.RequiredArgsConstructor;

/** Kept separate from ApprovalService to avoid a circular dependency with EmployeeService. */
@Component
@RequiredArgsConstructor
public class PendingApprovalChecker implements PendingWorkChecker {
    private final ApprovalTaskRepository repository;

    @Override
    public boolean hasPendingApprovals(Long approverId) {
        return repository.existsByApproverIdAndStatus(approverId, ApprovalStatus.PENDING);
    }
}

package com.icici.leavemanagement.common.port;

/**
 * Implemented by the approval module, so the employee module can check for open approvals
 * without depending on it directly.
 */
public interface PendingWorkChecker {
    boolean hasPendingApprovals(Long approverId);
}

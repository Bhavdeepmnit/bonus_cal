package com.icici.leavemanagement.reimbursement;

public enum ReimbursementStatus {
    PENDING,
    APPROVED,
    REJECTED;

    public static final String PATTERN = "PENDING|APPROVED|REJECTED";
}

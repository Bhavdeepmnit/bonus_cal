package com.icici.leavemanagement.employee;

public enum Role {
    EMPLOYEE, MANAGER, HR;

    /** Regex used in @Pattern on request DTOs, so bad values give a clear validation error. */
    public static final String PATTERN = "EMPLOYEE|MANAGER|HR";
}

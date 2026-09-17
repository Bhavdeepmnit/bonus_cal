package com.icici.leavemanagement.policy;

public enum LeaveType {
    CASUAL, SICK, EARNED;

    /** Regex used in @Pattern on request DTOs, so bad values give a clear validation error. */
    public static final String PATTERN = "CASUAL|SICK|EARNED";
}

package com.icici.leavemanagement.request;

import com.icici.leavemanagement.policy.LeaveType;

/** remaining = annualQuota + carriedForward - used - pending (pending days are reserved). */
public record LeaveBalanceResponse(LeaveType leaveType, int annualQuota, int carriedForward,
        int used, int pending, int remaining) {
}

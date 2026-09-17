package com.icici.leavemanagement.request;

import com.icici.leavemanagement.employee.Employee;

/**
 * Events published by the request module. The approval and notification modules listen for them
 * (synchronously, inside the same transaction), so this module does not depend on them.
 */
public final class LeaveEvents {

    private LeaveEvents() {
    }

    /** Leave passed the policy check and is waiting for the manager. */
    public record Submitted(LeaveRequest leave, Employee employee) {
    }

    /** Leave was over the quota. */
    public record RejectedByPolicy(LeaveRequest leave) {
    }

    /** Manager approved or rejected the leave. */
    public record Decided(LeaveRequest leave, String comments) {
    }

    /** Employee (or HR) cancelled a pending or future approved leave. */
    public record Cancelled(LeaveRequest leave, Employee employee, LeaveStatus previousStatus) {
    }
}

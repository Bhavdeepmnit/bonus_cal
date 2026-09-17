package com.icici.leavemanagement.request;

/** Counts for one employee and year (used later by the bonus rules). */
public record LeaveSummaryResponse(long rejectedCount, long unapprovedCount) {
}

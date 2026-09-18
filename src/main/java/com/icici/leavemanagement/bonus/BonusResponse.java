package com.icici.leavemanagement.bonus;

import java.math.BigDecimal;

public record BonusResponse(
    Long employeeId,
    int year,
    BigDecimal baseBonus,
    int appliedLeaveCount,
    int approvedLeaveCount,
    int approvedLeaveDays,
    int rejectedLeaveCount,
    int pendingLeaveCount,
    int cancelledLeaveCount,
    int reimbursementCount,
    int approvedReimbursementCount,
    BigDecimal approvedReimbursementAmount,
    int pendingReimbursementCount,
    BigDecimal pendingReimbursementAmount,
    int rejectedReimbursementCount,
    BigDecimal rejectedReimbursementAmount,
    BigDecimal appliedLeavePenalty,
    BigDecimal approvedLeaveDayPenalty,
    BigDecimal rejectedLeavePenalty,
    BigDecimal pendingLeavePenalty,
    BigDecimal reimbursementFrequencyPenalty,
    BigDecimal approvedReimbursementReward,
    BigDecimal pendingReimbursementPenalty,
    BigDecimal rejectedReimbursementPenalty,
    BigDecimal bonusAmount
) {}
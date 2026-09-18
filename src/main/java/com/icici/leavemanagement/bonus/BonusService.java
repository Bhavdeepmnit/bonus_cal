package com.icici.leavemanagement.bonus;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.icici.leavemanagement.common.exception.NotAllowedException;
import com.icici.leavemanagement.common.exception.ResourceNotFoundException;
import com.icici.leavemanagement.common.security.CurrentUser;
import com.icici.leavemanagement.employee.Employee;
import com.icici.leavemanagement.employee.EmployeeRepository;
import com.icici.leavemanagement.reimbursement.Reimbursement;
import com.icici.leavemanagement.reimbursement.ReimbursementRepository;
import com.icici.leavemanagement.reimbursement.ReimbursementStatus;
import com.icici.leavemanagement.request.LeaveRequest;
import com.icici.leavemanagement.request.LeaveRequestRepository;
import com.icici.leavemanagement.request.LeaveStatus;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BonusService {
    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

    private final LeaveRequestRepository leaveRepository;
    private final ReimbursementRepository reimbursementRepository;
    private final EmployeeRepository employeeRepository;
    private final CurrentUser currentUser;

    @Value("${app.bonus.base-amount:1000.00}")
    private BigDecimal baseBonus;
    @Value("${app.bonus.applied-leave-penalty:5.00}")
    private BigDecimal appliedLeaveRate;
    @Value("${app.bonus.approved-leave-day-penalty:10.00}")
    private BigDecimal approvedLeaveDayRate;
    @Value("${app.bonus.rejected-leave-penalty:20.00}")
    private BigDecimal rejectedLeaveRate;
    @Value("${app.bonus.pending-leave-penalty:5.00}")
    private BigDecimal pendingLeaveRate;
    @Value("${app.bonus.reimbursement-frequency-penalty:2.00}")
    private BigDecimal reimbursementFrequencyRate;
    @Value("${app.bonus.approved-reimbursement-reward-rate:0.02}")
    private BigDecimal approvedReimbursementRewardRate;
    @Value("${app.bonus.pending-reimbursement-penalty:10.00}")
    private BigDecimal pendingReimbursementRate;
    @Value("${app.bonus.rejected-reimbursement-penalty:15.00}")
    private BigDecimal rejectedReimbursementRate;

    @Transactional(readOnly = true)
    public BonusResponse calculate(Long employeeId, int year) {
        Employee employee = employeeRepository.findById(employeeId)
            .orElseThrow(() -> new ResourceNotFoundException("Employee", employeeId));
        if (!currentUser.get().canView(employee)) {
            throw new NotAllowedException("You can only view your own bonus and the bonus of direct reports");
        }

        List<LeaveRequest> leaves = leaveRepository.findByEmployeeIdAndStartDateBetween(
            employeeId, LocalDate.of(year, 1, 1), LocalDate.of(year, 12, 31));
        List<Reimbursement> reimbursements = reimbursementRepository.findByEmployeeIdAndCreatedAtBetween(
            employeeId, LocalDateTime.of(year, 1, 1, 0, 0), LocalDateTime.of(year + 1, 1, 1, 0, 0).minusNanos(1));

        int approvedLeaveCount = countLeaves(leaves, LeaveStatus.APPROVED);
        int approvedLeaveDays = leaves.stream().filter(leave -> leave.getStatus() == LeaveStatus.APPROVED)
            .mapToInt(LeaveRequest::getDays).sum();
        int rejectedLeaveCount = leaves.stream().filter(this::isRejected).mapToInt(leave -> 1).sum();
        int pendingLeaveCount = countLeaves(leaves, LeaveStatus.PENDING);
        int cancelledLeaveCount = countLeaves(leaves, LeaveStatus.CANCELLED);

        BigDecimal approvedReimbursementAmount = sumAmount(reimbursements, ReimbursementStatus.APPROVED);
        BigDecimal pendingReimbursementAmount = sumAmount(reimbursements, ReimbursementStatus.PENDING);
        BigDecimal rejectedReimbursementAmount = sumAmount(reimbursements, ReimbursementStatus.REJECTED);
        int approvedReimbursementCount = countReimbursements(reimbursements, ReimbursementStatus.APPROVED);
        int pendingReimbursementCount = countReimbursements(reimbursements, ReimbursementStatus.PENDING);
        int rejectedReimbursementCount = countReimbursements(reimbursements, ReimbursementStatus.REJECTED);

        BigDecimal appliedLeavePenalty = money(appliedLeaveRate.multiply(BigDecimal.valueOf(leaves.size())));
        BigDecimal approvedLeaveDayPenalty = money(approvedLeaveDayRate.multiply(BigDecimal.valueOf(approvedLeaveDays)));
        BigDecimal rejectedLeavePenalty = money(rejectedLeaveRate.multiply(BigDecimal.valueOf(rejectedLeaveCount)));
        BigDecimal pendingLeavePenalty = money(pendingLeaveRate.multiply(BigDecimal.valueOf(pendingLeaveCount)));
        BigDecimal reimbursementFrequencyPenalty = money(reimbursementFrequencyRate.multiply(BigDecimal.valueOf(reimbursements.size())));
        BigDecimal approvedReimbursementReward = money(approvedReimbursementAmount.multiply(approvedReimbursementRewardRate));
        BigDecimal pendingReimbursementPenalty = money(pendingReimbursementRate.multiply(BigDecimal.valueOf(pendingReimbursementCount)));
        BigDecimal rejectedReimbursementPenalty = money(rejectedReimbursementRate.multiply(BigDecimal.valueOf(rejectedReimbursementCount)));

        BigDecimal calculated = baseBonus.add(approvedReimbursementReward)
            .subtract(appliedLeavePenalty).subtract(approvedLeaveDayPenalty)
            .subtract(rejectedLeavePenalty).subtract(pendingLeavePenalty)
            .subtract(reimbursementFrequencyPenalty).subtract(pendingReimbursementPenalty)
            .subtract(rejectedReimbursementPenalty);
        BigDecimal bonusAmount = money(calculated.max(ZERO));

        return new BonusResponse(employeeId, year, money(baseBonus), leaves.size(), approvedLeaveCount,
            approvedLeaveDays, rejectedLeaveCount, pendingLeaveCount, cancelledLeaveCount, reimbursements.size(),
            approvedReimbursementCount, approvedReimbursementAmount, pendingReimbursementCount, pendingReimbursementAmount,
            rejectedReimbursementCount, rejectedReimbursementAmount, appliedLeavePenalty, approvedLeaveDayPenalty,
            rejectedLeavePenalty, pendingLeavePenalty, reimbursementFrequencyPenalty, approvedReimbursementReward,
            pendingReimbursementPenalty, rejectedReimbursementPenalty, bonusAmount);
    }

    private boolean isRejected(LeaveRequest leave) {
        return leave.getStatus() == LeaveStatus.REJECTED || leave.getStatus() == LeaveStatus.REJECTED_BY_POLICY;
    }

    private static int countLeaves(List<LeaveRequest> leaves, LeaveStatus status) {
        return (int) leaves.stream().filter(leave -> leave.getStatus() == status).count();
    }

    private static int countReimbursements(List<Reimbursement> reimbursements, ReimbursementStatus status) {
        return (int) reimbursements.stream().filter(item -> item.getStatus() == status).count();
    }

    private static BigDecimal sumAmount(List<Reimbursement> reimbursements, ReimbursementStatus status) {
        return money(reimbursements.stream().filter(item -> item.getStatus() == status)
            .map(Reimbursement::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    private static BigDecimal money(BigDecimal amount) {
        return amount.setScale(2, RoundingMode.HALF_UP);
    }
}
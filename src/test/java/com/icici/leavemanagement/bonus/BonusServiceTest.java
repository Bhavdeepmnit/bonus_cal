package com.icici.leavemanagement.bonus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.icici.leavemanagement.common.security.CurrentUser;
import com.icici.leavemanagement.employee.Employee;
import com.icici.leavemanagement.employee.EmployeeRepository;
import com.icici.leavemanagement.reimbursement.Reimbursement;
import com.icici.leavemanagement.reimbursement.ReimbursementRepository;
import com.icici.leavemanagement.reimbursement.ReimbursementStatus;
import com.icici.leavemanagement.request.LeaveRequest;
import com.icici.leavemanagement.request.LeaveRequestRepository;
import com.icici.leavemanagement.request.LeaveStatus;
import com.icici.leavemanagement.policy.LeaveType;

@ExtendWith(MockitoExtension.class)
class BonusServiceTest {
    @Mock private LeaveRequestRepository leaveRepository;
    @Mock private ReimbursementRepository reimbursementRepository;
    @Mock private EmployeeRepository employeeRepository;
    @Mock private CurrentUser currentUser;
    @InjectMocks private BonusService service;

    private final Employee employee = new Employee();

    @BeforeEach
    void setUp() {
        employee.setId(7L);
        employee.setName("Test Employee");
        employee.setActive(true);
        employee.setRole(com.icici.leavemanagement.employee.Role.EMPLOYEE);
        ReflectionTestUtils.setField(service, "baseBonus", new BigDecimal("1000.00"));
        ReflectionTestUtils.setField(service, "appliedLeaveRate", new BigDecimal("5.00"));
        ReflectionTestUtils.setField(service, "approvedLeaveDayRate", new BigDecimal("10.00"));
        ReflectionTestUtils.setField(service, "rejectedLeaveRate", new BigDecimal("20.00"));
        ReflectionTestUtils.setField(service, "pendingLeaveRate", new BigDecimal("5.00"));
        ReflectionTestUtils.setField(service, "reimbursementFrequencyRate", new BigDecimal("2.00"));
        ReflectionTestUtils.setField(service, "approvedReimbursementRewardRate", new BigDecimal("0.02"));
        ReflectionTestUtils.setField(service, "pendingReimbursementRate", new BigDecimal("10.00"));
        ReflectionTestUtils.setField(service, "rejectedReimbursementRate", new BigDecimal("15.00"));
        when(employeeRepository.findById(7L)).thenReturn(java.util.Optional.of(employee));
        when(currentUser.get()).thenReturn(employee);
    }

    @Test
    void calculatesMetricsAndBonusFromAllStatuses() {
        when(leaveRepository.findByEmployeeIdAndStartDateBetween(7L, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31)))
            .thenReturn(List.of(leave(LeaveStatus.APPROVED, 2), leave(LeaveStatus.REJECTED_BY_POLICY, 1),
                leave(LeaveStatus.PENDING, 1)));
        when(reimbursementRepository.findByEmployeeIdAndCreatedAtBetween(7L,
            LocalDateTime.of(2026, 1, 1, 0, 0), LocalDateTime.of(2027, 1, 1, 0, 0).minusNanos(1)))
            .thenReturn(List.of(reimbursement(ReimbursementStatus.APPROVED, "1000.00"),
                reimbursement(ReimbursementStatus.PENDING, "200.00"), reimbursement(ReimbursementStatus.REJECTED, "50.00")));

        BonusResponse result = service.calculate(7L, 2026);

        assertThat(result.appliedLeaveCount()).isEqualTo(3);
        assertThat(result.approvedLeaveCount()).isEqualTo(1);
        assertThat(result.approvedLeaveDays()).isEqualTo(2);
        assertThat(result.rejectedLeaveCount()).isEqualTo(1);
        assertThat(result.pendingLeaveCount()).isEqualTo(1);
        assertThat(result.reimbursementCount()).isEqualTo(3);
        assertThat(result.approvedReimbursementAmount()).isEqualByComparingTo("1000.00");
        assertThat(result.bonusAmount()).isEqualByComparingTo("929.00");
    }

    private static LeaveRequest leave(LeaveStatus status, int days) {
        LeaveRequest leave = new LeaveRequest();
        leave.setEmployeeId(7L);
        leave.setLeaveType(LeaveType.CASUAL);
        leave.setStartDate(LocalDate.of(2026, 3, 1));
        leave.setEndDate(LocalDate.of(2026, 3, 2));
        leave.setDays(days);
        leave.setStatus(status);
        return leave;
    }

    private static Reimbursement reimbursement(ReimbursementStatus status, String amount) {
        Reimbursement reimbursement = new Reimbursement();
        reimbursement.setEmployeeId(7L);
        reimbursement.setAmount(new BigDecimal(amount));
        reimbursement.setStatus(status);
        reimbursement.setCreatedAt(LocalDateTime.of(2026, 3, 1, 10, 0));
        return reimbursement;
    }
}
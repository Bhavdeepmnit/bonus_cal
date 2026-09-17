package com.icici.leavemanagement.request;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.icici.leavemanagement.employee.Employee;
import com.icici.leavemanagement.employee.EmployeeService;
import com.icici.leavemanagement.exception.ConflictException;
import com.icici.leavemanagement.exception.InvalidRequestException;
import com.icici.leavemanagement.exception.NotAllowedException;
import com.icici.leavemanagement.exception.ResourceNotFoundException;
import com.icici.leavemanagement.holiday.HolidayService;
import com.icici.leavemanagement.holiday.WorkingDays;
import com.icici.leavemanagement.policy.LeaveEvaluationResponse;
import com.icici.leavemanagement.policy.LeavePolicy;
import com.icici.leavemanagement.policy.LeavePolicyService;
import com.icici.leavemanagement.policy.LeaveType;
import com.icici.leavemanagement.security.CurrentUser;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class LeaveRequestService {
    static final int MAX_CALENDAR_DAYS = 60;
    /** Pending days are reserved, so two pending requests cannot together exceed the quota. */
    private static final Set<LeaveStatus> RESERVED = EnumSet.of(LeaveStatus.PENDING, LeaveStatus.APPROVED);

    private final LeaveRequestRepository repository;
    private final EmployeeService employeeService;
    private final LeavePolicyService policyService;
    private final HolidayService holidayService;
    private final CurrentUser currentUser;
    private final ApplicationEventPublisher events;

    @Transactional
    public LeaveRequest apply(ApplyLeaveRequest req) {
        LocalDate start = req.getStartDate();
        LocalDate end = req.getEndDate();
        if (end.isBefore(start)) {
            throw new InvalidRequestException("endDate must not be before startDate");
        }
        if (start.getYear() != end.getYear()) {
            throw new InvalidRequestException("A leave request must start and end in the same year; split it into two requests");
        }
        if (ChronoUnit.DAYS.between(start, end) + 1 > MAX_CALENDAR_DAYS) {
            throw new InvalidRequestException("A single request cannot span more than " + MAX_CALENDAR_DAYS + " calendar days");
        }

        // Lock the employee row: parallel requests from the same employee run one after the other
        Employee employee = employeeService.getByIdForUpdate(currentUser.get().getId());
        if (employee.getManagerId() == null) {
            throw new InvalidRequestException("You have no manager assigned, so nobody can approve your leave. Ask HR to set one.");
        }
        if (repository.existsByEmployeeIdAndStatusInAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                employee.getId(), RESERVED, end, start)) {
            throw new ConflictException("You already have pending or approved leave between " + start + " and " + end);
        }

        int days = WorkingDays.count(start, end, holidayService.datesBetween(start, end));
        if (days == 0) {
            throw new InvalidRequestException("The selected dates fall entirely on weekends or holidays");
        }

        LeaveType leaveType = LeaveType.valueOf(req.getLeaveType());
        LeavePolicy policy = policyService.getPolicy(employee.getGrade(), leaveType);
        int used = daysTaken(employee.getId(), leaveType, start.getYear(), RESERVED);
        int carried = carriedForward(employee, policy, start.getYear());
        LeaveEvaluationResponse evaluation = policyService.evaluate(policy, days, used, carried);

        LeaveRequest leave = new LeaveRequest();
        leave.setEmployeeId(employee.getId());
        leave.setLeaveType(leaveType);
        leave.setStartDate(start);
        leave.setEndDate(end);
        leave.setDays(days);
        leave.setStatus(evaluation.isApproved() ? LeaveStatus.PENDING : LeaveStatus.REJECTED_BY_POLICY);
        leave.setReason(req.getReason());
        leave.setPolicyRemarks(evaluation.getReason());
        leave.setCreatedAt(LocalDateTime.now());
        LeaveRequest saved = repository.save(leave);

        // Approval module creates the manager's task; notification module informs people
        events.publishEvent(saved.getStatus() == LeaveStatus.PENDING
            ? new LeaveEvents.Submitted(saved, employee)
            : new LeaveEvents.RejectedByPolicy(saved));
        return saved;
    }

    /** Pending leave, or approved leave that has not started yet, can be cancelled by its owner or HR. */
    @Transactional
    public LeaveRequest cancel(Long id) {
        Employee me = currentUser.get();
        LeaveRequest leave = getById(id);
        if (!leave.getEmployeeId().equals(me.getId()) && !me.isHr()) {
            throw new NotAllowedException("Only the employee who applied (or HR) can cancel this leave");
        }
        LeaveStatus previous = leave.getStatus();
        if (previous == LeaveStatus.APPROVED && !leave.getStartDate().isAfter(LocalDate.now())) {
            throw new ConflictException("Approved leave that has already started cannot be cancelled");
        }
        if (previous != LeaveStatus.PENDING && previous != LeaveStatus.APPROVED) {
            throw new ConflictException("Leave request " + id + " is " + previous + " and cannot be cancelled");
        }
        leave.setStatus(LeaveStatus.CANCELLED);
        LeaveRequest saved = repository.save(leave);
        events.publishEvent(new LeaveEvents.Cancelled(saved, employeeService.getById(saved.getEmployeeId()), previous));
        return saved;
    }

    /** Called by the approval module when the manager decides. Only PENDING requests can change. */
    @Transactional
    public LeaveRequest applyDecision(Long id, boolean approved, String comments) {
        LeaveRequest leave = getById(id);
        if (leave.getStatus() != LeaveStatus.PENDING) {
            throw new ConflictException("Leave request " + id + " is already " + leave.getStatus());
        }
        leave.setStatus(approved ? LeaveStatus.APPROVED : LeaveStatus.REJECTED);
        LeaveRequest saved = repository.save(leave);
        events.publishEvent(new LeaveEvents.Decided(saved, comments));
        return saved;
    }

    public LeaveRequest getVisible(Long id) {
        LeaveRequest leave = getById(id);
        checkCanView(leave.getEmployeeId());
        return leave;
    }

    public LeaveRequest getById(Long id) {
        return repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Leave request", id));
    }

    public Page<LeaveRequest> getMine(Pageable pageable) {
        return repository.findByEmployeeId(currentUser.get().getId(), pageable);
    }

    public Page<LeaveRequest> getByEmployee(Long employeeId, Pageable pageable) {
        checkCanView(employeeId);
        return repository.findByEmployeeId(employeeId, pageable);
    }

    public LeaveSummaryResponse getSummary(Long employeeId, int year) {
        validateYear(year);
        checkCanView(employeeId);
        List<LeaveRequest> requests = repository.findByEmployeeIdAndStartDateBetween(
            employeeId, LocalDate.of(year, 1, 1), LocalDate.of(year, 12, 31));

        long rejected = requests.stream()
            .filter(r -> r.getStatus() == LeaveStatus.REJECTED || r.getStatus() == LeaveStatus.REJECTED_BY_POLICY)
            .count();
        long unapproved = requests.stream().filter(r -> r.getStatus() == LeaveStatus.PENDING).count();
        return new LeaveSummaryResponse(rejected, unapproved);
    }

    /** employeeId null = the logged-in user. */
    public List<LeaveBalanceResponse> getBalance(Long employeeId, int year) {
        validateYear(year);
        Employee employee = employeeId == null ? currentUser.get() : checkCanView(employeeId);
        return policyService.getByGrade(employee.getGrade()).stream()
            .map(policy -> {
                int used = daysTaken(employee.getId(), policy.getLeaveType(), year, Set.of(LeaveStatus.APPROVED));
                int pending = daysTaken(employee.getId(), policy.getLeaveType(), year, Set.of(LeaveStatus.PENDING));
                int carried = carriedForward(employee, policy, year);
                int remaining = Math.max(policy.getAnnualQuota() + carried - used - pending, 0);
                return new LeaveBalanceResponse(policy.getLeaveType(), policy.getAnnualQuota(), carried, used, pending, remaining);
            })
            .toList();
    }

    private Employee checkCanView(Long employeeId) {
        Employee target = employeeService.getById(employeeId);
        if (!currentUser.get().canView(target)) {
            throw new NotAllowedException("You can only view leave of yourself and your direct reports");
        }
        return target;
    }

    private int daysTaken(Long employeeId, LeaveType type, int year, Collection<LeaveStatus> statuses) {
        return repository.findByEmployeeIdAndLeaveTypeAndStatusInAndStartDateBetween(
                employeeId, type, statuses, LocalDate.of(year, 1, 1), LocalDate.of(year, 12, 31))
            .stream().mapToInt(LeaveRequest::getDays).sum();
    }

    /** No carry-forward in (or before) the joining year. */
    private int carriedForward(Employee employee, LeavePolicy policy, int year) {
        if (employee.getJoiningDate() == null || employee.getJoiningDate().getYear() >= year) {
            return 0;
        }
        int usedLastYear = daysTaken(employee.getId(), policy.getLeaveType(), year - 1, Set.of(LeaveStatus.APPROVED));
        return policyService.carryForward(policy, usedLastYear);
    }

    private static void validateYear(int year) {
        if (year < 2000 || year > 2100) {
            throw new InvalidRequestException("year must be between 2000 and 2100");
        }
    }
}

package com.icici.leavemanagement.request;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.icici.leavemanagement.policy.LeaveType;

public interface LeaveRequestRepository extends JpaRepository<LeaveRequest, Long> {
    Page<LeaveRequest> findByEmployeeId(Long employeeId, Pageable pageable);

    List<LeaveRequest> findByEmployeeIdAndStartDateBetween(Long employeeId, LocalDate from, LocalDate to);

    List<LeaveRequest> findByEmployeeIdAndLeaveTypeAndStatusInAndStartDateBetween(
        Long employeeId, LeaveType leaveType, Collection<LeaveStatus> statuses, LocalDate from, LocalDate to);

    /** True if the employee already has a leave in one of the given statuses that overlaps [start, end]. */
    boolean existsByEmployeeIdAndStatusInAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
        Long employeeId, Collection<LeaveStatus> statuses, LocalDate end, LocalDate start);
}

package com.icici.leavemanagement.policy;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface LeavePolicyRepository extends JpaRepository<LeavePolicy, Long> {
    Optional<LeavePolicy> findByGradeAndLeaveType(String grade, LeaveType leaveType);

    List<LeavePolicy> findByGradeOrderByLeaveType(String grade);
}

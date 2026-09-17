package com.icici.leavemanagement.approval;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

public interface ApprovalTaskRepository extends JpaRepository<ApprovalTask, Long> {
    List<ApprovalTask> findByApproverIdAndStatusOrderByIdAsc(Long approverId, ApprovalStatus status);

    Optional<ApprovalTask> findByLeaveRequestId(Long leaveRequestId);

    boolean existsByApproverIdAndStatus(Long approverId, ApprovalStatus status);

    /** Row lock: two clicks on "approve" at the same time cannot both succeed. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from ApprovalTask t where t.id = :id")
    Optional<ApprovalTask> findByIdForUpdate(@Param("id") Long id);
}

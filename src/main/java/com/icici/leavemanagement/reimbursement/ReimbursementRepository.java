package com.icici.leavemanagement.reimbursement;

import java.util.Collection;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReimbursementRepository extends JpaRepository<Reimbursement, Long> {
    Page<Reimbursement> findByEmployeeId(Long employeeId, Pageable pageable);

    Page<Reimbursement> findByEmployeeIdIn(Collection<Long> employeeIds, Pageable pageable);

    List<Reimbursement> findByEmployeeIdAndCreatedAtBetween(Long employeeId, LocalDateTime from, LocalDateTime to);
}

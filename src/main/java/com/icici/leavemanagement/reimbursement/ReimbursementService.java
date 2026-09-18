package com.icici.leavemanagement.reimbursement;

import java.time.LocalDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.icici.leavemanagement.employee.Employee;
import com.icici.leavemanagement.employee.EmployeeRepository;
import com.icici.leavemanagement.common.exception.ConflictException;
import com.icici.leavemanagement.common.exception.NotAllowedException;
import com.icici.leavemanagement.common.exception.ResourceNotFoundException;
import com.icici.leavemanagement.common.security.CurrentUser;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ReimbursementService {
    private static final Logger log = LoggerFactory.getLogger(ReimbursementService.class);

    private final ReimbursementRepository repository;
    private final EmployeeRepository employeeRepository;
    private final CurrentUser currentUser;

    @Transactional
    public Reimbursement create(ReimbursementRequest request) {
        Employee me = currentUser.get();
        Reimbursement reimbursement = new Reimbursement();
        reimbursement.setEmployeeId(me.getId());
        reimbursement.setEmployeeName(me.getName());
        reimbursement.setTravelType(request.getTravelType());
        reimbursement.setSource(request.getSource());
        reimbursement.setDestination(request.getDestination());
        reimbursement.setAmount(request.getAmount());
        reimbursement.setStatus(ReimbursementStatus.PENDING);
        reimbursement.setCreatedAt(LocalDateTime.now());
        reimbursement.setUpdatedAt(reimbursement.getCreatedAt());
        Reimbursement saved = repository.save(reimbursement);
        log.info("Created reimbursement {} for employee {}", saved.getId(), me.getId());
        return saved;
    }

    public Page<Reimbursement> list(Pageable pageable) {
        Employee me = currentUser.get();
        if (me.isHr()) {
            return repository.findAll(pageable);
        }
        if (employeeRepository.existsByManagerIdAndActiveTrue(me.getId())) {
            return repository.findByEmployeeIdIn(employeeRepository.findByManagerId(me.getId(), Pageable.unpaged())
                .map(Employee::getId)
                .toList(), pageable);
        }
        return repository.findByEmployeeId(me.getId(), pageable);
    }

    public Reimbursement getVisible(Long id) {
        Reimbursement reimbursement = getById(id);
        checkCanView(reimbursement);
        return reimbursement;
    }

    @Transactional
    public Reimbursement update(Long id, ReimbursementRequest request) {
        Reimbursement reimbursement = getById(id);
        Employee me = currentUser.get();
        checkOwnerOrHr(reimbursement, me);
        if (reimbursement.getStatus() != ReimbursementStatus.PENDING) {
            throw new ConflictException("Only pending reimbursements can be updated");
        }
        reimbursement.setTravelType(request.getTravelType());
        reimbursement.setSource(request.getSource());
        reimbursement.setDestination(request.getDestination());
        reimbursement.setAmount(request.getAmount());
        reimbursement.setUpdatedAt(LocalDateTime.now());
        Reimbursement saved = repository.save(reimbursement);
        log.info("Updated reimbursement {} by employee {}", id, me.getId());
        return saved;
    }

    @Transactional
    public Reimbursement decide(Long id, ReimbursementDecisionRequest request) {
        Reimbursement reimbursement = getById(id);
        Employee me = currentUser.get();
        if (!me.isHr()) {
            throw new NotAllowedException("Only HR can approve or reject reimbursements");
        }
        if (reimbursement.getStatus() != ReimbursementStatus.PENDING) {
            throw new ConflictException("Reimbursement " + id + " is already " + reimbursement.getStatus());
        }
        reimbursement.setStatus(ReimbursementStatus.valueOf(request.getStatus()));
        reimbursement.setUpdatedAt(LocalDateTime.now());
        Reimbursement saved = repository.save(reimbursement);
        log.info("Reimbursement {} was {} by HR employee {}", id, saved.getStatus(), me.getId());
        return saved;
    }

    @Transactional
    public void delete(Long id) {
        Reimbursement reimbursement = getById(id);
        Employee me = currentUser.get();
        checkOwnerOrHr(reimbursement, me);
        if (reimbursement.getStatus() != ReimbursementStatus.PENDING) {
            throw new ConflictException("Only pending reimbursements can be deleted");
        }
        repository.delete(reimbursement);
        log.info("Deleted reimbursement {} by employee {}", id, me.getId());
    }

    private Reimbursement getById(Long id) {
        return repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Reimbursement", id));
    }

    private void checkCanView(Reimbursement reimbursement) {
        Employee owner = employeeRepository.findById(reimbursement.getEmployeeId())
            .orElseThrow(() -> new ResourceNotFoundException("Employee", reimbursement.getEmployeeId()));
        if (!currentUser.get().canView(owner)) {
            throw new NotAllowedException("You can only view your own reimbursements and direct reports");
        }
    }

    private static void checkOwnerOrHr(Reimbursement reimbursement, Employee me) {
        if (!reimbursement.getEmployeeId().equals(me.getId()) && !me.isHr()) {
            throw new NotAllowedException("Only the employee who submitted this reimbursement or HR can change it");
        }
    }
}

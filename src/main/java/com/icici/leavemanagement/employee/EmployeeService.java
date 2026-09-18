package com.icici.leavemanagement.employee;

import java.time.LocalDate;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.icici.leavemanagement.common.exception.ConflictException;
import com.icici.leavemanagement.common.exception.InvalidRequestException;
import com.icici.leavemanagement.common.exception.NotAllowedException;
import com.icici.leavemanagement.common.exception.ResourceNotFoundException;
import com.icici.leavemanagement.common.port.PendingWorkChecker;
import com.icici.leavemanagement.common.security.CurrentUser;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EmployeeService {
    private static final int MAX_MANAGER_CHAIN = 100;

    private final EmployeeRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final CurrentUser currentUser;
    private final PendingWorkChecker pendingWorkChecker;

    @Transactional
    public EmployeeResponse create(CreateEmployeeRequest req) {
        if (repository.existsByEmailIgnoreCase(req.getEmail())) {
            throw new ConflictException("An employee with email " + req.getEmail() + " already exists");
        }
        validateManager(null, req.getManagerId());

        Employee e = new Employee();
        e.setName(req.getName());
        e.setEmail(req.getEmail().toLowerCase());
        e.setPasswordHash(passwordEncoder.encode(req.getPassword()));
        e.setGrade(req.getGrade());
        e.setRole(Role.valueOf(req.getRole()));
        e.setManagerId(req.getManagerId());
        e.setJoiningDate(req.getJoiningDate() != null ? req.getJoiningDate() : LocalDate.now());
        return EmployeeResponse.from(repository.save(e));
    }

    @Transactional
    public EmployeeResponse update(Long id, UpdateEmployeeRequest req) {
        Employee e = getById(id);
        Role newRole = Role.valueOf(req.getRole());

        if (newRole == Role.EMPLOYEE && e.getRole() != Role.EMPLOYEE && repository.existsByManagerIdAndActiveTrue(id)) {
            throw new ConflictException("Employee " + id + " still manages active employees; reassign them before changing the role to EMPLOYEE");
        }
        if (e.isHr() && newRole != Role.HR && isLastActiveHr(e)) {
            throw new ConflictException("Employee " + id + " is the last active HR user and must keep the HR role");
        }
        validateManager(id, req.getManagerId());

        e.setName(req.getName());
        e.setGrade(req.getGrade());
        e.setRole(newRole);
        e.setManagerId(req.getManagerId());
        return EmployeeResponse.from(repository.save(e));
    }

    /** Soft delete: the employee can no longer log in, but their leave history stays. */
    @Transactional
    public void deactivate(Long id) {
        Employee e = getById(id);
        if (!e.isActive()) {
            throw new ConflictException("Employee " + id + " is already deactivated");
        }
        if (e.getId().equals(currentUser.get().getId())) {
            throw new InvalidRequestException("You cannot deactivate your own account");
        }
        if (repository.existsByManagerIdAndActiveTrue(id)) {
            throw new ConflictException("Employee " + id + " still manages active employees; reassign them first");
        }
        if (pendingWorkChecker.hasPendingApprovals(id)) {
            throw new ConflictException("Employee " + id + " has pending leave approvals; decide them first");
        }
        if (e.isHr() && isLastActiveHr(e)) {
            throw new ConflictException("Employee " + id + " is the last active HR user");
        }
        e.setActive(false);
        repository.save(e);
    }

    @Transactional
    public void changePassword(ChangePasswordRequest req) {
        Employee me = currentUser.get();
        if (!passwordEncoder.matches(req.getCurrentPassword(), me.getPasswordHash())) {
            throw new InvalidRequestException("Current password is incorrect");
        }
        if (req.getCurrentPassword().equals(req.getNewPassword())) {
            throw new InvalidRequestException("New password must be different from the current password");
        }
        me.setPasswordHash(passwordEncoder.encode(req.getNewPassword()));
        repository.save(me);
    }

    /** HR sees everyone, a manager sees their direct reports. */
    public Page<EmployeeResponse> list(Pageable pageable) {
        Employee me = currentUser.get();
        Page<Employee> page = me.isHr() ? repository.findAll(pageable) : repository.findByManagerId(me.getId(), pageable);
        return page.map(EmployeeResponse::from);
    }

    public EmployeeResponse me() {
        return EmployeeResponse.from(currentUser.get());
    }

    public EmployeeResponse getVisible(Long id) {
        Employee target = getById(id);
        if (!currentUser.get().canView(target)) {
            throw new NotAllowedException("You can only view yourself and your direct reports");
        }
        return EmployeeResponse.from(target);
    }

    public Employee getById(Long id) {
        return repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Employee", id));
    }

    /** Locks the employee row until the transaction ends. */
    public Employee getByIdForUpdate(Long id) {
        return repository.findByIdForUpdate(id).orElseThrow(() -> new ResourceNotFoundException("Employee", id));
    }

    /**
     * The manager must exist, be active, have role MANAGER or HR, and must not be the employee
     * or anyone who (directly or indirectly) reports to the employee.
     * employeeId is null when creating a new employee.
     */
    void validateManager(Long employeeId, Long managerId) {
        if (managerId == null) {
            return;
        }
        if (managerId.equals(employeeId)) {
            throw new InvalidRequestException("An employee cannot be their own manager");
        }
        Employee manager = repository.findById(managerId)
            .orElseThrow(() -> new InvalidRequestException("Manager with id " + managerId + " does not exist"));
        if (!manager.isActive()) {
            throw new InvalidRequestException("Manager " + managerId + " is deactivated");
        }
        if (manager.getRole() == Role.EMPLOYEE) {
            throw new InvalidRequestException("Employee " + managerId + " has role EMPLOYEE and cannot be a manager");
        }
        if (employeeId == null) {
            return;
        }
        Long next = manager.getManagerId();
        for (int steps = 0; next != null && steps < MAX_MANAGER_CHAIN; steps++) {
            if (next.equals(employeeId)) {
                throw new InvalidRequestException("Manager " + managerId + " reports (directly or indirectly) to employee "
                    + employeeId + "; this would create a reporting loop");
            }
            next = repository.findById(next).map(Employee::getManagerId).orElse(null);
        }
    }

    private boolean isLastActiveHr(Employee e) {
        return e.isActive() && repository.countByRoleAndActiveTrue(Role.HR) <= 1;
    }
}

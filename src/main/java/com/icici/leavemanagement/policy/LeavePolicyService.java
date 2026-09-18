package com.icici.leavemanagement.policy;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.icici.leavemanagement.common.exception.ConflictException;
import com.icici.leavemanagement.common.exception.InvalidRequestException;
import com.icici.leavemanagement.common.exception.ResourceNotFoundException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class LeavePolicyService {
    private final LeavePolicyRepository repository;

    @Transactional
    public LeavePolicy create(LeavePolicyRequest req) {
        LeaveType type = LeaveType.valueOf(req.getLeaveType());
        if (repository.findByGradeAndLeaveType(req.getGrade(), type).isPresent()) {
            throw new ConflictException("A policy for grade " + req.getGrade() + " / type " + type + " already exists");
        }
        return repository.save(new LeavePolicy(null, type, req.getGrade(), req.getAnnualQuota(), req.getCarryForwardLimit()));
    }

    @Transactional
    public LeavePolicy update(Long id, UpdateLeavePolicyRequest req) {
        LeavePolicy policy = getById(id);
        policy.setAnnualQuota(req.getAnnualQuota());
        policy.setCarryForwardLimit(req.getCarryForwardLimit());
        return repository.save(policy);
    }

    @Transactional
    public void delete(Long id) {
        repository.delete(getById(id));
    }

    public List<LeavePolicy> getAll() { return repository.findAll(); }

    public List<LeavePolicy> getByGrade(String grade) { return repository.findByGradeOrderByLeaveType(grade); }

    public LeavePolicy getById(Long id) {
        return repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Leave policy", id));
    }

    public LeavePolicy getPolicy(String grade, LeaveType leaveType) {
        return repository.findByGradeAndLeaveType(grade, leaveType)
            .orElseThrow(() -> new InvalidRequestException("No policy found for grade " + grade + " / type " + leaveType));
    }

    public LeaveEvaluationResponse evaluate(LeaveEvaluationRequest req) {
        LeavePolicy policy = getPolicy(req.getGrade(), LeaveType.valueOf(req.getLeaveType()));
        int carried = req.getCarriedForwardDays() != null ? req.getCarriedForwardDays() : 0;
        return evaluate(policy, req.getRequestedDays(), req.getAlreadyUsedDays(), carried);
    }

    /** Available = annual quota + carried-forward days. The request fits if it is not more than what is left. */
    public LeaveEvaluationResponse evaluate(LeavePolicy policy, int requestedDays, int alreadyUsedDays, int carriedForwardDays) {
        int remaining = Math.max(policy.getAnnualQuota() + carriedForwardDays - alreadyUsedDays, 0);
        if (requestedDays > remaining) {
            return new LeaveEvaluationResponse(false,
                "Requested " + requestedDays + " days but only " + remaining + " remaining");
        }
        return new LeaveEvaluationResponse(true, "Within policy limit");
    }

    /** Unused days from last year, capped at the policy's carry-forward limit. */
    public int carryForward(LeavePolicy policy, int usedLastYear) {
        int unused = Math.max(policy.getAnnualQuota() - usedLastYear, 0);
        return Math.min(unused, policy.getCarryForwardLimit());
    }
}

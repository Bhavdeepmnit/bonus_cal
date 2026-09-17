package com.icici.leavemanagement.approval;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.icici.leavemanagement.employee.Employee;
import com.icici.leavemanagement.exception.ConflictException;
import com.icici.leavemanagement.exception.InvalidRequestException;
import com.icici.leavemanagement.exception.NotAllowedException;
import com.icici.leavemanagement.exception.ResourceNotFoundException;
import com.icici.leavemanagement.request.LeaveEvents;
import com.icici.leavemanagement.request.LeaveRequestService;
import com.icici.leavemanagement.security.CurrentUser;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ApprovalService {
    private static final int MAX_COMMENT_LENGTH = 255;

    private final ApprovalTaskRepository repository;
    private final LeaveRequestService leaveRequestService;
    private final CurrentUser currentUser;

    /** Runs inside the apply-leave transaction: if this fails, the leave request is not saved either. */
    @EventListener
    public void onLeaveSubmitted(LeaveEvents.Submitted event) {
        repository.save(new ApprovalTask(event.leave().getId(), event.employee().getManagerId()));
    }

    @EventListener
    public void onLeaveCancelled(LeaveEvents.Cancelled event) {
        repository.findByLeaveRequestId(event.leave().getId())
            .filter(task -> task.getStatus() == ApprovalStatus.PENDING)
            .ifPresent(task -> {
                task.setStatus(ApprovalStatus.CANCELLED);
                task.setDecidedAt(LocalDateTime.now());
                repository.save(task);
            });
    }

    @Transactional
    public ApprovalTask decide(Long taskId, String decision, String comments) {
        ApprovalStatus status = parseDecision(decision);
        if (comments != null && comments.length() > MAX_COMMENT_LENGTH) {
            throw new InvalidRequestException("comments must be at most " + MAX_COMMENT_LENGTH + " characters");
        }
        Employee me = currentUser.get();
        ApprovalTask task = repository.findByIdForUpdate(taskId)
            .orElseThrow(() -> new ResourceNotFoundException("Approval task", taskId));
        if (!task.getApproverId().equals(me.getId())) {
            throw new NotAllowedException("Task " + taskId + " can only be decided by its approver (employee " + task.getApproverId() + ")");
        }
        if (task.getStatus() != ApprovalStatus.PENDING) {
            throw new ConflictException("Task " + taskId + " is already " + task.getStatus());
        }

        leaveRequestService.applyDecision(task.getLeaveRequestId(), status == ApprovalStatus.APPROVED, comments);
        task.setStatus(status);
        task.setComments(comments);
        task.setDecidedAt(LocalDateTime.now());
        return repository.save(task);
    }

    public ApprovalTask getVisible(Long id) {
        ApprovalTask task = repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Approval task", id));
        Employee me = currentUser.get();
        if (!task.getApproverId().equals(me.getId()) && !me.isHr()) {
            throw new NotAllowedException("You can only view your own approval tasks");
        }
        return task;
    }

    public List<ApprovalTask> getMyPending() {
        return repository.findByApproverIdAndStatusOrderByIdAsc(currentUser.get().getId(), ApprovalStatus.PENDING);
    }

    private static ApprovalStatus parseDecision(String decision) {
        if ("APPROVED".equalsIgnoreCase(decision)) return ApprovalStatus.APPROVED;
        if ("REJECTED".equalsIgnoreCase(decision)) return ApprovalStatus.REJECTED;
        throw new InvalidRequestException("decision must be APPROVED or REJECTED");
    }
}

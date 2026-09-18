package com.icici.leavemanagement.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.icici.leavemanagement.common.exception.ResourceNotFoundException;
import com.icici.leavemanagement.request.LeaveEvents;
import com.icici.leavemanagement.request.LeaveRequest;
import com.icici.leavemanagement.common.security.CurrentUser;

import lombok.RequiredArgsConstructor;

/**
 * In-app notifications for leave events. Each notification is also logged;
 * an email sender can be added here later without touching the other modules.
 */
@Service
@RequiredArgsConstructor
public class NotificationService {
    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);
    private static final int MAX_MESSAGE_LENGTH = 500;

    private final NotificationRepository repository;
    private final CurrentUser currentUser;

    @EventListener
    public void onSubmitted(LeaveEvents.Submitted event) {
        LeaveRequest leave = event.leave();
        notify(event.employee().getManagerId(), event.employee().getName() + " applied for " + describe(leave)
            + ". Please approve or reject it.");
        notify(leave.getEmployeeId(), "Your " + describe(leave) + " was submitted and is waiting for approval.");
    }

    @EventListener
    public void onRejectedByPolicy(LeaveEvents.RejectedByPolicy event) {
        LeaveRequest leave = event.leave();
        notify(leave.getEmployeeId(), "Your " + describe(leave) + " was rejected by policy: " + leave.getPolicyRemarks());
    }

    @EventListener
    public void onDecided(LeaveEvents.Decided event) {
        LeaveRequest leave = event.leave();
        String comments = event.comments() == null || event.comments().isBlank() ? "" : " Comments: " + event.comments();
        notify(leave.getEmployeeId(), "Your " + describe(leave) + " was " + leave.getStatus() + "." + comments);
    }

    @EventListener
    public void onCancelled(LeaveEvents.Cancelled event) {
        LeaveRequest leave = event.leave();
        if (event.employee().getManagerId() != null) {
            notify(event.employee().getManagerId(), event.employee().getName() + " cancelled " + describe(leave)
                + " (was " + event.previousStatus() + ").");
        }
    }

    public Page<Notification> getMine(boolean unreadOnly, Pageable pageable) {
        Long me = currentUser.get().getId();
        return unreadOnly ? repository.findByRecipientIdAndReadFalse(me, pageable) : repository.findByRecipientId(me, pageable);
    }

    @Transactional
    public Notification markRead(Long id) {
        Notification n = repository.findByIdAndRecipientId(id, currentUser.get().getId())
            .orElseThrow(() -> new ResourceNotFoundException("Notification", id));
        n.setRead(true);
        return repository.save(n);
    }

    @Transactional
    public int markAllRead() {
        return repository.markAllRead(currentUser.get().getId());
    }

    private void notify(Long recipientId, String message) {
        String text = message.length() > MAX_MESSAGE_LENGTH ? message.substring(0, MAX_MESSAGE_LENGTH) : message;
        repository.save(new Notification(recipientId, text));
        log.info("Notification to employee {}: {}", recipientId, text);
    }

    private static String describe(LeaveRequest leave) {
        return leave.getLeaveType() + " leave request #" + leave.getId() + " (" + leave.getStartDate() + " to "
            + leave.getEndDate() + ", " + leave.getDays() + " working day" + (leave.getDays() == 1 ? "" : "s") + ")";
    }
}

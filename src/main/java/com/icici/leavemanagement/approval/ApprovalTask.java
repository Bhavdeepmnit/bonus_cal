package com.icici.leavemanagement.approval;

import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "approval_tasks")
@Getter @Setter @NoArgsConstructor
public class ApprovalTask {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long leaveRequestId;
    private Long approverId;
    @Enumerated(EnumType.STRING)
    private ApprovalStatus status;
    private String comments;
    private LocalDateTime decidedAt;
    @Version
    private Long version;

    public ApprovalTask(Long leaveRequestId, Long approverId) {
        this.leaveRequestId = leaveRequestId;
        this.approverId = approverId;
        this.status = ApprovalStatus.PENDING;
    }
}

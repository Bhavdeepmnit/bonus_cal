package com.icici.leavemanagement.request;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.icici.leavemanagement.policy.LeaveType;

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
@Table(name = "leave_requests")
@Getter @Setter @NoArgsConstructor
public class LeaveRequest {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long employeeId;
    @Enumerated(EnumType.STRING)
    private LeaveType leaveType;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer days;          // working days (weekends and holidays excluded)
    @Enumerated(EnumType.STRING)
    private LeaveStatus status;
    private String reason;         // why the employee wants leave
    private String policyRemarks;  // result message from the policy check
    private LocalDateTime createdAt;
    @Version
    private Long version;
}

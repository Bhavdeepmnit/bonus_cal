package com.icici.leavemanagement.policy;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "leave_policies")
@Data @NoArgsConstructor @AllArgsConstructor
public class LeavePolicy {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Enumerated(EnumType.STRING)
    private LeaveType leaveType;       // CASUAL, SICK, EARNED
    private String grade;              // L1, L2, L3
    private Integer annualQuota;       // working days per year
    private Integer carryForwardLimit; // max unused days moved to the next year
}

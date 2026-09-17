package com.icici.leavemanagement.policy;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @AllArgsConstructor @NoArgsConstructor
public class LeaveEvaluationResponse {
    private boolean approved;
    private String reason;
}

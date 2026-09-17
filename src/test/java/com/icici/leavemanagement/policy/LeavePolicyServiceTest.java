package com.icici.leavemanagement.policy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.icici.leavemanagement.exception.ConflictException;
import com.icici.leavemanagement.exception.InvalidRequestException;

@ExtendWith(MockitoExtension.class)
class LeavePolicyServiceTest {

    @Mock
    private LeavePolicyRepository repository;

    @InjectMocks
    private LeavePolicyService service;

    private final LeavePolicy casualL1 = new LeavePolicy(1L, LeaveType.CASUAL, "L1", 10, 3);

    @Test
    void evaluate_withinQuota_isApproved() {
        LeaveEvaluationResponse res = service.evaluate(casualL1, 4, 6, 0);
        assertThat(res.isApproved()).isTrue();
        assertThat(res.getReason()).isEqualTo("Within policy limit");
    }

    @Test
    void evaluate_overQuota_isRejectedWithRemainingDays() {
        LeaveEvaluationResponse res = service.evaluate(casualL1, 5, 6, 0);
        assertThat(res.isApproved()).isFalse();
        assertThat(res.getReason()).isEqualTo("Requested 5 days but only 4 remaining");
    }

    @Test
    void evaluate_carriedForwardDaysAddToTheQuota() {
        assertThat(service.evaluate(casualL1, 5, 6, 1).isApproved()).isTrue();
    }

    @Test
    void evaluate_neverReportsNegativeRemaining() {
        assertThat(service.evaluate(casualL1, 1, 15, 0).getReason()).isEqualTo("Requested 1 days but only 0 remaining");
    }

    @Test
    void carryForward_isUnusedDaysCappedAtTheLimit() {
        assertThat(service.carryForward(casualL1, 2)).isEqualTo(3);   // 8 unused, limit 3
        assertThat(service.carryForward(casualL1, 9)).isEqualTo(1);   // 1 unused
        assertThat(service.carryForward(casualL1, 12)).isZero();      // over-used
    }

    @Test
    void getPolicy_missing_throwsInvalidRequest() {
        when(repository.findByGradeAndLeaveType("L9", LeaveType.SICK)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.getPolicy("L9", LeaveType.SICK))
            .isInstanceOf(InvalidRequestException.class)
            .hasMessage("No policy found for grade L9 / type SICK");
    }

    @Test
    void create_duplicate_throwsConflict() {
        when(repository.findByGradeAndLeaveType("L1", LeaveType.CASUAL)).thenReturn(Optional.of(casualL1));
        assertThatThrownBy(() -> service.create(new LeavePolicyRequest("CASUAL", "L1", 5, 0)))
            .isInstanceOf(ConflictException.class);
    }
}

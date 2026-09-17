package com.icici.leavemanagement.employee;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.icici.leavemanagement.exception.ConflictException;
import com.icici.leavemanagement.exception.InvalidRequestException;
import com.icici.leavemanagement.security.CurrentUser;

@ExtendWith(MockitoExtension.class)
class EmployeeServiceTest {

    @Mock private EmployeeRepository repository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private CurrentUser currentUser;
    @Mock private PendingWorkChecker pendingWorkChecker;

    @InjectMocks
    private EmployeeService service;

    @Test
    void noManager_isAllowed() {
        assertThatCode(() -> service.validateManager(1L, null)).doesNotThrowAnyException();
    }

    @Test
    void ownManager_isRejected() {
        assertThatThrownBy(() -> service.validateManager(5L, 5L))
            .isInstanceOf(InvalidRequestException.class)
            .hasMessage("An employee cannot be their own manager");
        verifyNoInteractions(repository);
    }

    @Test
    void unknownManager_isRejected() {
        when(repository.findById(9L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.validateManager(null, 9L)).hasMessage("Manager with id 9 does not exist");
    }

    @Test
    void deactivatedManager_isRejected() {
        Employee m = employee(2L, Role.MANAGER, null);
        m.setActive(false);
        when(repository.findById(2L)).thenReturn(Optional.of(m));
        assertThatThrownBy(() -> service.validateManager(1L, 2L)).hasMessage("Manager 2 is deactivated");
    }

    @Test
    void managerWithEmployeeRole_isRejected() {
        when(repository.findById(2L)).thenReturn(Optional.of(employee(2L, Role.EMPLOYEE, null)));
        assertThatThrownBy(() -> service.validateManager(1L, 2L))
            .hasMessage("Employee 2 has role EMPLOYEE and cannot be a manager");
    }

    @Test
    void indirectReportingLoop_isRejected() {
        // 2 reports to 3, 3 reports to 1 -> making 2 the manager of 1 creates a loop
        when(repository.findById(2L)).thenReturn(Optional.of(employee(2L, Role.MANAGER, 3L)));
        when(repository.findById(3L)).thenReturn(Optional.of(employee(3L, Role.MANAGER, 1L)));
        assertThatThrownBy(() -> service.validateManager(1L, 2L))
            .isInstanceOf(InvalidRequestException.class)
            .hasMessageContaining("reporting loop");
    }

    @Test
    void validChain_isAccepted() {
        when(repository.findById(2L)).thenReturn(Optional.of(employee(2L, Role.MANAGER, 3L)));
        when(repository.findById(3L)).thenReturn(Optional.of(employee(3L, Role.HR, null)));
        assertThatCode(() -> service.validateManager(1L, 2L)).doesNotThrowAnyException();
    }

    @Test
    void create_withDuplicateEmail_isConflict() {
        when(repository.existsByEmailIgnoreCase("a@b.com")).thenReturn(true);
        CreateEmployeeRequest req = new CreateEmployeeRequest("A", "a@b.com", "Password1", "L1", "EMPLOYEE", null, null);
        assertThatThrownBy(() -> service.create(req)).isInstanceOf(ConflictException.class);
    }

    private static Employee employee(Long id, Role role, Long managerId) {
        Employee e = new Employee();
        e.setId(id);
        e.setRole(role);
        e.setManagerId(managerId);
        return e;
    }
}

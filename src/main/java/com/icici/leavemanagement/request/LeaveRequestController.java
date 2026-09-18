package com.icici.leavemanagement.request;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "4. Leave requests")
@RestController
@RequestMapping("/api/leave-requests")
@RequiredArgsConstructor
public class LeaveRequestController {
    private final LeaveRequestService service;

    // POST /api/leave-requests  -> 201 Created, status PENDING or REJECTED_BY_POLICY
    @Operation(summary = "Apply for leave (as the logged-in employee)")
    @PostMapping
    public ResponseEntity<LeaveRequest> apply(@Valid @RequestBody ApplyLeaveRequest request) {
        LeaveRequest created = service.apply(request);
        return ResponseEntity.created(URI.create("/api/leave-requests/" + created.getId())).body(created);
    }

    // GET /api/leave-requests/my?page=0&size=20
    @Operation(summary = "My leave requests")
    @GetMapping("/my")
    public PagedModel<LeaveRequest> getMine(
            @ParameterObject @PageableDefault(size = 20, sort = "startDate", direction = Sort.Direction.DESC) Pageable pageable) {
        return new PagedModel<>(service.getMine(pageable));
    }

    // GET /api/leave-requests/1  (owner, their manager, or HR)
    @Operation(summary = "Get leave request (owner, manager or HR)")
    @GetMapping("/{id}")
    public LeaveRequest getById(@Parameter(example = "1") @PathVariable Long id) { return service.getVisible(id); }

    // PUT /api/leave-requests/1/cancel  (owner or HR)
    @Operation(summary = "Cancel leave (pending, or approved and not started)")
    @PutMapping("/{id}/cancel")
    public LeaveRequest cancel(@Parameter(example = "3") @PathVariable Long id) { return service.cancel(id); }

    // GET /api/leave-requests/employee/2?page=0&size=20
    @Operation(summary = "Leave requests of an employee (self, manager or HR)")
    @GetMapping("/employee/{employeeId}")
    public PagedModel<LeaveRequest> getByEmployee(@Parameter(example = "3") @PathVariable Long employeeId,
            @ParameterObject @PageableDefault(size = 20, sort = "startDate", direction = Sort.Direction.DESC) Pageable pageable) {
        return new PagedModel<>(service.getByEmployee(employeeId, pageable));
    }

    // GET /api/leave-requests/summary/2?year=2026
    @Operation(summary = "Yearly summary: rejected and pending counts")
    @GetMapping("/summary/{employeeId}")
    public LeaveSummaryResponse getSummary(@Parameter(example = "3") @PathVariable Long employeeId,
                                           @Parameter(example = "2026") @RequestParam int year) {
        return service.getSummary(employeeId, year);
    }

    // GET /api/leave-requests/balance?year=2026  (own balance; year defaults to this year)
    @Operation(summary = "My leave balance")
    @GetMapping("/balance")
    public List<LeaveBalanceResponse> getMyBalance(@Parameter(example = "2026") @RequestParam(required = false) Integer year) {
        return service.getBalance(null, orThisYear(year));
    }

    // GET /api/leave-requests/balance/2?year=2026
    @Operation(summary = "Leave balance of an employee (self, manager or HR)")
    @GetMapping("/balance/{employeeId}")
    public List<LeaveBalanceResponse> getBalance(@Parameter(example = "3") @PathVariable Long employeeId,
                                                 @Parameter(example = "2026") @RequestParam(required = false) Integer year) {
        return service.getBalance(employeeId, orThisYear(year));
    }

    private static int orThisYear(Integer year) {
        return year != null ? year : LocalDate.now().getYear();
    }
}

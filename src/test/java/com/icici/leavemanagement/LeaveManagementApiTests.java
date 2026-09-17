package com.icici.leavemanagement;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import com.jayway.jsonpath.JsonPath;

/**
 * End-to-end API tests, one section per user story (see README.md).
 * Runs on H2 with Flyway migrations; every test is rolled back.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles({ "h2", "test" })
@Transactional
class LeaveManagementApiTests {

    private static final String ADMIN = "admin@test.com";
    private static final String ADMIN_PASSWORD = "Admin@12345";
    private static final String PASSWORD = "Password1";

    private static final String MANAGER = "manager@test.com";
    private static final String MANAGER2 = "manager2@test.com";
    private static final String EMP = "emp@test.com";
    private static final String TEAMMATE = "teammate@test.com";

    /** Next year, so every date is in the future and in one year. */
    private static final int Y = LocalDate.now().getYear() + 1;
    private static final LocalDate MON = LocalDate.of(Y, 3, 1).with(TemporalAdjusters.firstInMonth(DayOfWeek.MONDAY));

    @Autowired
    private MockMvc mvc;

    private long managerId;
    private long manager2Id;
    private long empId;
    private long teammateId;

    @BeforeEach
    void createPeople() throws Exception {
        managerId = createEmployee("Priya", MANAGER, "L3", "MANAGER", null, null);
        manager2Id = createEmployee("Arjun", MANAGER2, "L3", "MANAGER", managerId, null);
        empId = createEmployee("Ravi", EMP, "L1", "EMPLOYEE", managerId, (Y - 3) + "-01-01");
        teammateId = createEmployee("Sita", TEAMMATE, "L1", "EMPLOYEE", managerId, null);
    }

    // ================= US-7 security =================

    @Test
    void loginIsRequired_andWrongPasswordIsRejected() throws Exception {
        mvc.perform(get("/api/employees/me"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.status").value(401));
        mvc.perform(get("/api/employees/me").with(httpBasic(EMP, "wrong-password")))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void healthAndApiDocs_arePublic() throws Exception {
        mvc.perform(get("/actuator/health")).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("UP"));
        mvc.perform(get("/v3/api-docs")).andExpect(status().isOk());
    }

    @Test
    void employeeRole_cannotUseHrOrManagerEndpoints() throws Exception {
        send(post("/api/employees"), EMP, employeeJson("X", "x@test.com", "L1", "EMPLOYEE", null, null))
            .andExpect(status().isForbidden()).andExpect(jsonPath("$.status").value(403));
        send(get("/api/employees"), EMP).andExpect(status().isForbidden());
        send(get("/api/approvals/pending"), EMP).andExpect(status().isForbidden());
        send(post("/api/policies"), EMP, "{\"leaveType\":\"SICK\",\"grade\":\"L1\",\"annualQuota\":1,\"carryForwardLimit\":0}")
            .andExpect(status().isForbidden());
        send(post("/api/holidays"), EMP, "{\"date\":\"" + MON + "\",\"name\":\"X\"}").andExpect(status().isForbidden());
    }

    // ================= US-1 employees =================

    @Test
    void createEmployee_validationErrors() throws Exception {
        send(post("/api/employees"), ADMIN, "{\"name\":\"\",\"email\":\"bad\",\"password\":\"short\",\"grade\":\"L9\",\"role\":\"BOSS\"}")
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.fieldErrors.name").exists())
            .andExpect(jsonPath("$.fieldErrors.email").exists())
            .andExpect(jsonPath("$.fieldErrors.password").value("password must be 8 to 72 characters"))
            .andExpect(jsonPath("$.fieldErrors.grade").value("grade must be L1, L2 or L3"))
            .andExpect(jsonPath("$.fieldErrors.role").value("role must be EMPLOYEE, MANAGER or HR"));
    }

    @Test
    void createEmployee_businessRules() throws Exception {
        send(post("/api/employees"), ADMIN, employeeJson("Dup", "EMP@test.com", "L1", "EMPLOYEE", null, null))
            .andExpect(status().isConflict());
        send(post("/api/employees"), ADMIN, employeeJson("X", "x@test.com", "L1", "EMPLOYEE", 99999L, null))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Manager with id 99999 does not exist"));
        send(post("/api/employees"), ADMIN, employeeJson("X", "x@test.com", "L1", "EMPLOYEE", empId, null))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message", containsString("has role EMPLOYEE")));
    }

    @Test
    void viewingEmployees_respectsRoles() throws Exception {
        send(get("/api/employees/me"), EMP)
            .andExpect(jsonPath("$.email").value(EMP))
            .andExpect(jsonPath("$.passwordHash").doesNotExist());
        send(get("/api/employees/" + teammateId), EMP).andExpect(status().isForbidden());
        send(get("/api/employees/" + empId), MANAGER).andExpect(status().isOk());
        send(get("/api/employees/99999"), ADMIN).andExpect(status().isNotFound());
        send(get("/api/employees/abc"), ADMIN).andExpect(status().isBadRequest());

        // manager sees only direct reports, HR sees everyone; both paged
        send(get("/api/employees"), MANAGER)
            .andExpect(jsonPath("$.content", hasSize(3)))
            .andExpect(jsonPath("$.content[*].managerId", everyItem(org.hamcrest.Matchers.is((int) managerId))));
        send(get("/api/employees?page=0&size=2"), ADMIN)
            .andExpect(jsonPath("$.content", hasSize(2)))
            .andExpect(jsonPath("$.page.size").value(2))
            .andExpect(jsonPath("$.page.totalElements", greaterThanOrEqualTo(5)));
        send(get("/api/employees?sort=nope"), ADMIN).andExpect(status().isBadRequest());
    }

    @Test
    void updateEmployee_rules() throws Exception {
        // own manager
        send(put("/api/employees/" + managerId), ADMIN, updateJson("Priya", "L3", "MANAGER", managerId))
            .andExpect(status().isBadRequest());
        // loop: manager2 reports to manager, so manager cannot report to manager2
        send(put("/api/employees/" + managerId), ADMIN, updateJson("Priya", "L3", "MANAGER", manager2Id))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message", containsString("reporting loop")));
        // manager with reports cannot become EMPLOYEE
        send(put("/api/employees/" + managerId), ADMIN, updateJson("Priya", "L3", "EMPLOYEE", null))
            .andExpect(status().isConflict());
        // last HR keeps the HR role
        long adminId = id(send(get("/api/employees/me"), ADMIN));
        send(put("/api/employees/" + adminId), ADMIN, updateJson("HR Admin", "L3", "MANAGER", null))
            .andExpect(status().isConflict());
        // valid change
        send(put("/api/employees/" + empId), ADMIN, updateJson("Ravi K", "L2", "EMPLOYEE", manager2Id))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.grade").value("L2"))
            .andExpect(jsonPath("$.managerId").value(manager2Id));
    }

    @Test
    void deactivateEmployee_rules() throws Exception {
        send(delete("/api/employees/" + managerId), ADMIN).andExpect(status().isConflict());   // has reports
        long adminId = id(send(get("/api/employees/me"), ADMIN));
        send(delete("/api/employees/" + adminId), ADMIN).andExpect(status().isBadRequest());   // self

        send(delete("/api/employees/" + teammateId), ADMIN).andExpect(status().isNoContent());
        send(delete("/api/employees/" + teammateId), ADMIN).andExpect(status().isConflict());
        send(get("/api/employees/me"), TEAMMATE).andExpect(status().isUnauthorized());          // cannot log in
    }

    @Test
    void managerWithPendingApprovals_cannotBeDeactivated() throws Exception {
        apply(EMP, "CASUAL", MON, MON).andExpect(status().isCreated());
        send(put("/api/employees/" + empId), ADMIN, updateJson("Ravi", "L1", "EMPLOYEE", manager2Id)).andExpect(status().isOk());
        send(put("/api/employees/" + teammateId), ADMIN, updateJson("Sita", "L1", "EMPLOYEE", manager2Id)).andExpect(status().isOk());
        send(put("/api/employees/" + manager2Id), ADMIN, updateJson("Arjun", "L3", "MANAGER", null)).andExpect(status().isOk());
        send(delete("/api/employees/" + managerId), ADMIN)
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.message", containsString("pending leave approvals")));
    }

    @Test
    void changePassword() throws Exception {
        send(put("/api/employees/me/password"), EMP, "{\"currentPassword\":\"nope-nope\",\"newPassword\":\"NewPassword1\"}")
            .andExpect(status().isBadRequest());
        send(put("/api/employees/me/password"), EMP, "{\"currentPassword\":\"" + PASSWORD + "\",\"newPassword\":\"NewPassword1\"}")
            .andExpect(status().isNoContent());
        send(get("/api/employees/me"), EMP).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/employees/me").with(httpBasic(EMP, "NewPassword1"))).andExpect(status().isOk());
    }

    // ================= US-2 policies =================

    @Test
    void policies_crudAndEvaluate() throws Exception {
        send(get("/api/policies"), EMP).andExpect(jsonPath("$", hasSize(9)));
        send(post("/api/policies"), ADMIN, "{\"leaveType\":\"SICK\",\"grade\":\"L1\",\"annualQuota\":5,\"carryForwardLimit\":0}")
            .andExpect(status().isConflict());
        send(put("/api/policies/99999"), ADMIN, "{\"annualQuota\":5,\"carryForwardLimit\":0}").andExpect(status().isNotFound());

        send(post("/api/policies/evaluate"), EMP,
                "{\"grade\":\"L1\",\"leaveType\":\"CASUAL\",\"requestedDays\":4,\"alreadyUsedDays\":10,\"carriedForwardDays\":3}")
            .andExpect(jsonPath("$.approved").value(false))
            .andExpect(jsonPath("$.reason").value("Requested 4 days but only 3 remaining"));
        send(post("/api/policies/evaluate"), EMP, "{\"grade\":\"L7\",\"leaveType\":\"SICK\",\"requestedDays\":1,\"alreadyUsedDays\":0}")
            .andExpect(status().isBadRequest());

        net.minidev.json.JSONArray sickIds = JsonPath.read(body(send(get("/api/policies"), EMP)),
            "$[?(@.grade=='L1' && @.leaveType=='SICK')].id");
        long sickL1 = ((Number) sickIds.get(0)).longValue();
        send(put("/api/policies/" + sickL1), ADMIN, "{\"annualQuota\":20,\"carryForwardLimit\":2}")
            .andExpect(jsonPath("$.annualQuota").value(20));
        send(delete("/api/policies/" + sickL1), ADMIN).andExpect(status().isNoContent());
        apply(EMP, "SICK", MON, MON)
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("No policy found for grade L1 / type SICK"));
    }

    // ================= US-3 holidays & working days =================

    @Test
    void holidays_andWorkingDayCounting() throws Exception {
        send(post("/api/holidays"), ADMIN, "{\"date\":\"" + MON.plusDays(2) + "\",\"name\":\"Festival\"}")
            .andExpect(status().isCreated());
        send(post("/api/holidays"), ADMIN, "{\"date\":\"" + MON.plusDays(2) + "\",\"name\":\"Again\"}")
            .andExpect(status().isConflict());
        send(get("/api/holidays?year=" + Y), EMP).andExpect(jsonPath("$[*].name", hasItem("Festival")));

        apply(EMP, "CASUAL", MON, MON.plusDays(4)).andExpect(jsonPath("$.days").value(4));             // Mon-Fri minus holiday
        apply(EMP, "CASUAL", MON.plusDays(11), MON.plusDays(14)).andExpect(jsonPath("$.days").value(2)); // Fri-Mon
        apply(EMP, "CASUAL", MON.plusDays(19), MON.plusDays(20))                                          // Sat-Sun
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("The selected dates fall entirely on weekends or holidays"));
    }

    // ================= US-4 apply + US-5 approve + US-6 balance/summary =================

    @Test
    void fullFlow_applyApproveBalanceSummaryNotifications() throws Exception {
        long leaveId = id(apply(EMP, "CASUAL", MON, MON.plusDays(4))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value("PENDING"))
            .andExpect(jsonPath("$.days").value(5)));

        // joined 3 years ago, nothing used last year -> 3 days carried forward (limit 3)
        send(get("/api/leave-requests/balance?year=" + Y), EMP)
            .andExpect(jsonPath("$[?(@.leaveType=='CASUAL')].carriedForward").value(3))
            .andExpect(jsonPath("$[?(@.leaveType=='CASUAL')].pending").value(5))
            .andExpect(jsonPath("$[?(@.leaveType=='CASUAL')].remaining").value(8));

        long taskId = ((Number) JsonPath.read(body(send(get("/api/approvals/pending"), MANAGER)
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].leaveRequestId").value(leaveId))), "$[0].id")).longValue();

        send(get("/api/notifications?unreadOnly=true"), MANAGER)
            .andExpect(jsonPath("$.content[0].message", containsString("Ravi applied for CASUAL leave request #" + leaveId)));

        send(put("/api/approvals/" + taskId + "/decide?decision=APPROVED&comments=Enjoy"), MANAGER)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("APPROVED"))
            .andExpect(jsonPath("$.decidedAt").exists());

        send(get("/api/leave-requests/" + leaveId), EMP).andExpect(jsonPath("$.status").value("APPROVED"));
        send(get("/api/approvals/pending"), MANAGER).andExpect(jsonPath("$", hasSize(0)));
        send(get("/api/notifications"), EMP)
            .andExpect(jsonPath("$.content[0].message", containsString("was APPROVED. Comments: Enjoy")));
        send(get("/api/leave-requests/balance/" + empId + "?year=" + Y), MANAGER)
            .andExpect(jsonPath("$[?(@.leaveType=='CASUAL')].used").value(5))
            .andExpect(jsonPath("$[?(@.leaveType=='CASUAL')].pending").value(0))
            .andExpect(jsonPath("$[?(@.leaveType=='CASUAL')].remaining").value(8));
        send(get("/api/leave-requests/my"), EMP).andExpect(jsonPath("$.content", hasSize(1)));
        send(get("/api/leave-requests/summary/" + empId + "?year=" + Y), EMP)
            .andExpect(jsonPath("$.rejectedCount").value(0))
            .andExpect(jsonPath("$.unapprovedCount").value(0));
    }

    @Test
    void pendingDaysAreReserved_soQuotaCannotBeExceeded() throws Exception {
        // L1 CASUAL: 10 + 3 carried = 13
        apply(EMP, "CASUAL", MON, MON.plusDays(4)).andExpect(jsonPath("$.status").value("PENDING"));                     // 5
        apply(EMP, "CASUAL", MON.plusDays(7), MON.plusDays(11)).andExpect(jsonPath("$.status").value("PENDING"));        // 5
        apply(EMP, "CASUAL", MON.plusDays(14), MON.plusDays(16)).andExpect(jsonPath("$.status").value("PENDING"));       // 3
        apply(EMP, "CASUAL", MON.plusDays(17), MON.plusDays(17))
            .andExpect(jsonPath("$.status").value("REJECTED_BY_POLICY"))
            .andExpect(jsonPath("$.policyRemarks").value("Requested 1 days but only 0 remaining"));

        send(get("/api/notifications"), EMP)
            .andExpect(jsonPath("$.content[0].message", containsString("rejected by policy")));
        send(get("/api/leave-requests/summary/" + empId + "?year=" + Y), MANAGER)
            .andExpect(jsonPath("$.rejectedCount").value(1))
            .andExpect(jsonPath("$.unapprovedCount").value(3));
    }

    @Test
    void carryForward_dependsOnLastYearsUsage_andJoiningYear() throws Exception {
        // last year: 9 approved CASUAL days -> 1 unused -> 1 carried into this year
        LocalDate lastYearMon = LocalDate.of(Y - 1, 3, 1).with(TemporalAdjusters.firstInMonth(DayOfWeek.MONDAY));
        long leaveId = id(apply(EMP, "CASUAL", lastYearMon, lastYearMon.plusDays(10)).andExpect(jsonPath("$.days").value(9)));
        approveLeave(leaveId);

        send(get("/api/leave-requests/balance?year=" + Y), EMP)
            .andExpect(jsonPath("$[?(@.leaveType=='CASUAL')].carriedForward").value(1))
            .andExpect(jsonPath("$[?(@.leaveType=='CASUAL')].remaining").value(11));

        // teammate joined this year (Y - 1): nothing is carried into the joining year
        send(get("/api/leave-requests/balance?year=" + (Y - 1)), TEAMMATE)
            .andExpect(jsonPath("$[?(@.leaveType=='CASUAL')].carriedForward").value(0));
    }

    @Test
    void cancel_pendingAndFutureApprovedLeave() throws Exception {
        long pendingId = id(apply(EMP, "CASUAL", MON, MON));
        long taskId = firstPendingTaskId();

        send(put("/api/leave-requests/" + pendingId + "/cancel"), TEAMMATE).andExpect(status().isForbidden());
        send(put("/api/leave-requests/" + pendingId + "/cancel"), EMP)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("CANCELLED"));
        send(put("/api/leave-requests/" + pendingId + "/cancel"), EMP).andExpect(status().isConflict());
        send(get("/api/approvals/" + taskId), MANAGER).andExpect(jsonPath("$.status").value("CANCELLED"));
        send(get("/api/approvals/pending"), MANAGER).andExpect(jsonPath("$", hasSize(0)));
        send(get("/api/notifications"), MANAGER).andExpect(jsonPath("$.content[0].message", containsString("Ravi cancelled")));

        // approved leave in the future can be cancelled and the days come back
        long approvedId = id(apply(EMP, "CASUAL", MON.plusDays(7), MON.plusDays(8)));
        approveLeave(approvedId);
        send(put("/api/leave-requests/" + approvedId + "/cancel"), EMP).andExpect(jsonPath("$.status").value("CANCELLED"));
        send(get("/api/leave-requests/balance?year=" + Y), EMP)
            .andExpect(jsonPath("$[?(@.leaveType=='CASUAL')].used").value(0));

        // approved leave in the past cannot be cancelled
        LocalDate pastMon = LocalDate.of(Y - 3, 3, 1).with(TemporalAdjusters.firstInMonth(DayOfWeek.MONDAY));
        long pastId = id(apply(EMP, "SICK", pastMon, pastMon));
        approveLeave(pastId);
        send(put("/api/leave-requests/" + pastId + "/cancel"), EMP)
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.message").value("Approved leave that has already started cannot be cancelled"));
    }

    @Test
    void apply_errorCases() throws Exception {
        apply(EMP, "CASUAL", MON.plusDays(4), MON).andExpect(status().isBadRequest());
        apply(EMP, "CASUAL", LocalDate.of(Y, 12, 31), LocalDate.of(Y + 1, 1, 1)).andExpect(status().isBadRequest());
        apply(EMP, "CASUAL", MON, MON.plusDays(60))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message", containsString("60 calendar days")));
        apply(EMP, "VACATION", MON, MON)
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.fieldErrors.leaveType").exists());
        send(post("/api/leave-requests"), EMP, "{\"leaveType\":\"SICK\",\"startDate\":\"01-03-2027\",\"endDate\":\"" + MON + "\"}")
            .andExpect(status().isBadRequest());
        send(post("/api/leave-requests"), EMP, "{bad json").andExpect(status().isBadRequest());
        apply(ADMIN, "CASUAL", MON, MON)   // admin has no manager
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message", containsString("no manager")));

        apply(EMP, "CASUAL", MON, MON.plusDays(2)).andExpect(status().isCreated());
        apply(EMP, "SICK", MON.plusDays(2), MON.plusDays(3)).andExpect(status().isConflict());
    }

    @Test
    void leaveVisibility() throws Exception {
        long leaveId = id(apply(EMP, "CASUAL", MON, MON));
        send(get("/api/leave-requests/" + leaveId), TEAMMATE).andExpect(status().isForbidden());
        send(get("/api/leave-requests/" + leaveId), MANAGER).andExpect(status().isOk());
        send(get("/api/leave-requests/" + leaveId), ADMIN).andExpect(status().isOk());
        send(get("/api/leave-requests/employee/" + empId), TEAMMATE).andExpect(status().isForbidden());
        send(get("/api/leave-requests/employee/" + empId), MANAGER).andExpect(jsonPath("$.content", hasSize(1)));
        send(get("/api/leave-requests/summary/" + empId + "?year=1999"), MANAGER).andExpect(status().isBadRequest());
        send(get("/api/leave-requests/99999"), ADMIN).andExpect(status().isNotFound());
    }

    @Test
    void decide_errorCases() throws Exception {
        apply(EMP, "CASUAL", MON, MON);
        long taskId = firstPendingTaskId();
        String url = "/api/approvals/" + taskId + "/decide";

        send(put(url + "?decision=MAYBE"), MANAGER).andExpect(status().isBadRequest());
        send(put(url), MANAGER)
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Required parameter 'decision' is missing"));
        send(put(url + "?decision=APPROVED&comments=" + "x".repeat(256)), MANAGER).andExpect(status().isBadRequest());
        send(put(url + "?decision=APPROVED"), MANAGER2).andExpect(status().isForbidden());
        send(put(url + "?decision=APPROVED"), ADMIN).andExpect(status().isForbidden());   // HR is not the approver
        send(get("/api/approvals/" + taskId), ADMIN).andExpect(status().isOk());          // but HR can view it
        send(get("/api/approvals/" + taskId), MANAGER2).andExpect(status().isForbidden());
        send(put("/api/approvals/99999/decide?decision=APPROVED"), MANAGER).andExpect(status().isNotFound());

        send(put(url + "?decision=rejected"), MANAGER).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("REJECTED"));
        send(put(url + "?decision=APPROVED"), MANAGER).andExpect(status().isConflict());
    }

    // ================= US-8 notifications =================

    @Test
    void notifications_readAndReadAll() throws Exception {
        apply(EMP, "CASUAL", MON, MON);
        apply(EMP, "CASUAL", MON.plusDays(1), MON.plusDays(1));

        String list = body(send(get("/api/notifications?unreadOnly=true"), MANAGER).andExpect(jsonPath("$.content", hasSize(2))));
        long firstId = ((Number) JsonPath.read(list, "$.content[0].id")).longValue();

        send(put("/api/notifications/" + firstId + "/read"), EMP).andExpect(status().isNotFound());   // not theirs
        send(put("/api/notifications/" + firstId + "/read"), MANAGER).andExpect(jsonPath("$.read").value(true));
        send(get("/api/notifications?unreadOnly=true"), MANAGER).andExpect(jsonPath("$.content", hasSize(1)));
        send(put("/api/notifications/read-all"), MANAGER).andExpect(jsonPath("$.updated").value(1));
        send(get("/api/notifications?unreadOnly=true"), MANAGER).andExpect(jsonPath("$.content", hasSize(0)));
    }

    // ================= US-9 error format =================

    @Test
    void unknownUrlAndWrongMethod_returnJsonErrors() throws Exception {
        send(get("/api/nothing-here"), EMP).andExpect(status().isNotFound()).andExpect(jsonPath("$.status").value(404));
        send(delete("/api/leave-requests/my"), EMP).andExpect(status().isMethodNotAllowed());
    }

    // ================= helpers =================

    private long createEmployee(String name, String email, String grade, String role, Long managerId, String joiningDate)
            throws Exception {
        return id(send(post("/api/employees"), ADMIN, employeeJson(name, email, grade, role, managerId, joiningDate))
            .andExpect(status().isCreated()));
    }

    private static String employeeJson(String name, String email, String grade, String role, Long managerId, String joiningDate) {
        return "{\"name\":\"" + name + "\",\"email\":\"" + email + "\",\"password\":\"" + PASSWORD + "\",\"grade\":\"" + grade
            + "\",\"role\":\"" + role + "\",\"managerId\":" + managerId
            + (joiningDate != null ? ",\"joiningDate\":\"" + joiningDate + "\"" : "") + "}";
    }

    private static String updateJson(String name, String grade, String role, Long managerId) {
        return "{\"name\":\"" + name + "\",\"grade\":\"" + grade + "\",\"role\":\"" + role + "\",\"managerId\":" + managerId + "}";
    }

    private ResultActions apply(String user, String type, LocalDate start, LocalDate end) throws Exception {
        return send(post("/api/leave-requests"), user,
            "{\"leaveType\":\"" + type + "\",\"startDate\":\"" + start + "\",\"endDate\":\"" + end + "\",\"reason\":\"test\"}");
    }

    private void approveLeave(long leaveId) throws Exception {
        String tasks = body(send(get("/api/approvals/pending"), MANAGER));
        net.minidev.json.JSONArray ids = JsonPath.read(tasks, "$[?(@.leaveRequestId==" + leaveId + ")].id");
        send(put("/api/approvals/" + ((Number) ids.get(0)).longValue() + "/decide?decision=APPROVED"), MANAGER)
            .andExpect(status().isOk());
    }

    private long firstPendingTaskId() throws Exception {
        return ((Number) JsonPath.read(body(send(get("/api/approvals/pending"), MANAGER)), "$[0].id")).longValue();
    }

    private ResultActions send(MockHttpServletRequestBuilder request, String user) throws Exception {
        return mvc.perform(request.with(httpBasic(user, ADMIN.equals(user) ? ADMIN_PASSWORD : PASSWORD)));
    }

    private ResultActions send(MockHttpServletRequestBuilder request, String user, String json) throws Exception {
        return send(request.contentType(MediaType.APPLICATION_JSON).content(json), user);
    }

    private static String body(ResultActions result) throws Exception {
        return result.andReturn().getResponse().getContentAsString();
    }

    private static long id(ResultActions result) throws Exception {
        return ((Number) JsonPath.read(body(result), "$.id")).longValue();
    }
}

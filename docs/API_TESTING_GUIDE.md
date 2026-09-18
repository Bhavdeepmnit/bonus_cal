# API Testing Guide (Postman)

This guide tests **every API**, step by step, in the right order, with the exact data to send and the result you should get. It covers **80 requests** in 9 phases:

| Phase | Logged in as | What it tests |
|---|---|---|
| 0 | nobody | Health check, login required |
| 1 | HR admin | Create, list, update employees, plus their error cases |
| 2 | HR admin | Leave policies and holidays |
| 3 | Ravi (employee) | Apply for leave, balance, cancel, error cases |
| 4 | Sita (employee) | Privacy: cannot touch the leave of a teammate |
| 5 | Priya (manager) | Pending approvals, approve, reject, notifications |
| 6 | Ravi (employee) | Results of the decisions, summary, cancel approved leave, change password |
| 7 | HR admin | Oversight, deactivate an employee, error pages |
| 8 | HR admin | Calculate the yearly bonus from leave and reimbursement history |

The same steps, with the same numbers, are in the Postman collection `postman/leave-management.postman_collection.json`. Each request in it logs in as the right person, saves the IDs it creates, and **checks the result automatically**. The collection contains 80 requests, including the bonus calculation check.

---

## Before you start

### 1. Start the app
VS Code → Run and Debug → **Leave Management (MySQL)** → F5. Wait for `Started LeaveManagementApplication`.
Check: open http://localhost:8081/actuator/health. It should show `{"status":"UP"}`.

### 2. Use a fresh database
The expected IDs (2, 3, 4…) and numbers below assume the database contains **only the HR admin**. That is true right now. To start over later:
1. Stop the app.
2. In MySQL Workbench (connected as root), run:
   ```sql
   DROP DATABASE leave_management_db;
   CREATE DATABASE leave_management_db;
   ```
   The `leave_app` user keeps its rights on the database.
3. Start the app again. Flyway recreates the tables, the 9 policies and the HR admin.

### 3. Import into Postman
1. Postman → **Import** → select `D:\Java-learn\leave-management\postman\leave-management.postman_collection.json`.
2. Click the collection → **Variables** tab and set these current values:
  - `baseUrl`: `http://localhost:8081`
  - `adminEmail`: `ADMIN_EMAIL` from `D:\Java-learn\.env`
  - `adminPassword`: `ADMIN_PASSWORD` from `D:\Java-learn\.env`
  - `bonusYear`: the year you want to calculate, for example `2026`
  Then click **Save**.

### 4. Run it
- **Run everything automatically:** right-click the collection → **Run collection** → **Run**. All 80 requests should show green ✅.
- **Run step by step:** open the requests in order (0.1, 0.2, …) and click **Send**. Each response has a **Test Results** tab that shows whether it passed.

### Logins used in this guide

All requests use **Authorization → Basic Auth** (username = email).

| Person | Role | Email (username) | Password | Created in step |
|---|---|---|---|---|
| HR Admin | HR | `admin@company.com` | `ADMIN_PASSWORD` from `.env` | app start |
| Priya Sharma | MANAGER | `priya@company.com` | `Priya@123` | 1.2 |
| Ravi Kumar | EMPLOYEE (L1), reports to Priya | `ravi@company.com` | `Ravi@1234` (changes to `Ravi@5678` in step 6.7) | 1.3 |
| Sita Verma | EMPLOYEE (L1), reports to Priya | `sita@company.com` | `Sita@1234` | 1.4 |
| Arjun Mehta | MANAGER (L2), reports to Priya | `arjun@company.com` | `Arjun@123` | 1.5 |

**Base URL:** `http://localhost:8081`. For every request with a body, set **Body → raw → JSON** (Postman adds `Content-Type: application/json`).

**Dates used** (today is 17 Sep 2026; all leave dates are in the future):

| Date(s) | Why |
|---|---|
| Mon 28 Sep – Fri 2 Oct 2026 | 5 days, but 2 Oct is a holiday, so **4 working days** |
| Mon 2 – Fri 27 Nov 2026 | 20 working days, which is over the quota |
| Mon 12 Oct 2026 | 1 day of SICK leave |
| Mon 19 – Tue 20 Oct 2026 | 2 days of EARNED leave |
| Sat 10 – Sun 11 Oct 2026 | weekend only, so it must fail |

---

## Phase 0 — Public checks (no login)

In Postman: **Authorization → No Auth** (except step 0.3).

| Step | Method & URL | Login | Expected |
|---|---|---|---|
| 0.1 | `GET /actuator/health` | none | **200** `{"groups":["liveness","readiness"],"status":"UP"}` |
| 0.2 | `GET /api/employees/me` | none | **401** `"message": "Login required: send a valid email and password (HTTP Basic auth)…"` |
| 0.3 | `GET /api/employees/me` | `admin@company.com` / `wrong-password` | **401** (same message) |

Also open **http://localhost:8081/swagger-ui.html** in a browser: this is the interactive API page.

---

## Phase 1 — HR: employees

**Login for all steps: HR admin** (`admin@company.com` / `ADMIN_PASSWORD`).

### 1.1 Who am I
`GET /api/employees/me` → **200**
```json
{"id":1,"name":"HR Admin","email":"admin@company.com","grade":"L3","role":"HR","managerId":null,"joiningDate":"2026-09-17","active":true}
```

### 1.2 Create manager Priya
`POST /api/employees`
```json
{
  "name": "Priya Sharma",
  "email": "priya@company.com",
  "password": "Priya@123",
  "grade": "L3",
  "role": "MANAGER"
}
```
→ **201** `{"id":2, "name":"Priya Sharma", "role":"MANAGER", "managerId":null, …}` (the password is never returned)

### 1.3 Create employee Ravi (joined 2023, so he gets carry-forward)
`POST /api/employees`
```json
{
  "name": "Ravi Kumar",
  "email": "ravi@company.com",
  "password": "Ravi@1234",
  "grade": "L1",
  "role": "EMPLOYEE",
  "managerId": 2,
  "joiningDate": "2023-04-01"
}
```
→ **201** `{"id":3, …, "managerId":2, "joiningDate":"2023-04-01"}`

### 1.4 Create employee Sita
`POST /api/employees`
```json
{
  "name": "Sita Verma",
  "email": "sita@company.com",
  "password": "Sita@1234",
  "grade": "L1",
  "role": "EMPLOYEE",
  "managerId": 2
}
```
→ **201** `{"id":4, …}`. Without `joiningDate`, it defaults to today.

### 1.5 Create manager Arjun (reports to Priya)
`POST /api/employees`
```json
{
  "name": "Arjun Mehta",
  "email": "arjun@company.com",
  "password": "Arjun@123",
  "grade": "L2",
  "role": "MANAGER",
  "managerId": 2
}
```
→ **201** `{"id":5, …}`

### 1.6 List employees (paged, sorted)
`GET /api/employees?page=0&size=10&sort=name` → **200**
```json
{"content":[{"id":5,"name":"Arjun Mehta",…},{"id":1,"name":"HR Admin",…},{"id":2,"name":"Priya Sharma",…},{"id":3,"name":"Ravi Kumar",…},{"id":4,"name":"Sita Verma",…}],
 "page":{"size":10,"number":0,"totalElements":5,"totalPages":1}}
```
Try `size=2`, then `page=1`, to see paging.

### 1.7 Get one employee
`GET /api/employees/3` → **200** (Ravi)

### 1.8 Update employee
`PUT /api/employees/3`
```json
{
  "name": "Ravi Kumar Singh",
  "grade": "L1",
  "role": "EMPLOYEE",
  "managerId": 2
}
```
→ **200** `"name":"Ravi Kumar Singh"`

### Error cases

| Step | Request | Body | Expected |
|---|---|---|---|
| 1.9 | `POST /api/employees` | `{"name":"","email":"not-an-email","password":"123","grade":"L9","role":"BOSS"}` | **400** `"Validation failed"` with `fieldErrors` for name, email, password, grade and role |
| 1.10 | `POST /api/employees` | `{"name":"Copy","email":"RAVI@company.com","password":"Password1","grade":"L1","role":"EMPLOYEE","managerId":2}` | **409** `"An employee with email RAVI@company.com already exists"` |
| 1.11 | `POST /api/employees` | `{"name":"Nobody","email":"nobody@company.com","password":"Password1","grade":"L1","role":"EMPLOYEE","managerId":3}` | **400** `"Employee 3 has role EMPLOYEE and cannot be a manager"` |
| 1.12 | `PUT /api/employees/2` | `{"name":"Priya Sharma","grade":"L3","role":"MANAGER","managerId":2}` | **400** `"An employee cannot be their own manager"` |
| 1.13 | `PUT /api/employees/2` | `{"name":"Priya Sharma","grade":"L3","role":"MANAGER","managerId":5}` | **400** `"Manager 5 reports (directly or indirectly) to employee 2; this would create a reporting loop"` |
| 1.14 | `DELETE /api/employees/2` | — | **409** `"Employee 2 still manages active employees; reassign them first"` |
| 1.15 | `GET /api/employees/999999` | — | **404** `"Employee not found with id 999999"` |

---

## Phase 2 — HR: policies and holidays

**Login: HR admin.**

| Step | Request | Body | Expected |
|---|---|---|---|
| 2.1 | `GET /api/policies` | — | **200**: 9 policies. CASUAL/L1 has `id 1`, EARNED/L3 has `id 9` |
| 2.2 | `POST /api/policies` | `{"leaveType":"CASUAL","grade":"L1","annualQuota":12,"carryForwardLimit":2}` | **409** `"A policy for grade L1 / type CASUAL already exists"` |
| 2.3 | `PUT /api/policies/1` | `{"annualQuota":10,"carryForwardLimit":3}` | **200** `{"id":1,"leaveType":"CASUAL","grade":"L1","annualQuota":10,"carryForwardLimit":3}` |
| 2.4 | `DELETE /api/policies/9` | — | **204** (no body) |
| 2.5 | `POST /api/policies` | `{"leaveType":"EARNED","grade":"L3","annualQuota":21,"carryForwardLimit":8}` | **201** `{"id":10,…}` (the policy is back) |
| 2.6 | `POST /api/policies/evaluate` | `{"grade":"L1","leaveType":"CASUAL","requestedDays":12,"alreadyUsedDays":0,"carriedForwardDays":3}` | **200** `{"approved":true,"reason":"Within policy limit"}` |
| 2.7 | `POST /api/policies/evaluate` | `{"grade":"L1","leaveType":"CASUAL","requestedDays":14,"alreadyUsedDays":0,"carriedForwardDays":3}` | **200** `{"approved":false,"reason":"Requested 14 days but only 13 remaining"}` |
| 2.8 | `POST /api/holidays` | `{"date":"2026-10-02","name":"Gandhi Jayanti"}` | **201** `{"id":1,"holidayDate":"2026-10-02","name":"Gandhi Jayanti"}` |
| 2.9 | `POST /api/holidays` | `{"date":"2026-12-25","name":"Christmas"}` | **201** `{"id":2,…}` |
| 2.10 | `POST /api/holidays` | `{"date":"2026-10-02","name":"Duplicate"}` | **409** `"A holiday on 2026-10-02 already exists"` |
| 2.11 | `GET /api/holidays?year=2026` | — | **200**: 2 holidays |
| 2.12 | `DELETE /api/holidays/2` | — | **204** (Christmas removed; Gandhi Jayanti stays for the next phase) |

> The calculation behind 2.6 and 2.7: available = quota 10 + carried 3 − used 0 = **13**. So 12 days fits and 14 does not.

---

## Phase 3 — Employee Ravi: apply for leave

**Login: `ravi@company.com` / `Ravi@1234`.**

### 3.1 Who am I
`GET /api/employees/me` → **200** `{"id":3,"name":"Ravi Kumar Singh",…}`

### 3.2 Balance before applying
`GET /api/leave-requests/balance?year=2026` → **200**
```json
[
  {"leaveType":"CASUAL","annualQuota":10,"carriedForward":3,"used":0,"pending":0,"remaining":13},
  {"leaveType":"EARNED","annualQuota":15,"carriedForward":5,"used":0,"pending":0,"remaining":20},
  {"leaveType":"SICK",  "annualQuota":8, "carriedForward":0,"used":0,"pending":0,"remaining":8}
]
```
Ravi joined in 2023 and used nothing in 2025, so he carries forward the maximum: CASUAL 3, EARNED 5, SICK 0.

### 3.3 Apply CASUAL leave → PENDING
`POST /api/leave-requests`
```json
{
  "leaveType": "CASUAL",
  "startDate": "2026-09-28",
  "endDate": "2026-10-02",
  "reason": "Family function"
}
```
→ **201**
```json
{"id":1,"employeeId":3,"leaveType":"CASUAL","startDate":"2026-09-28","endDate":"2026-10-02","days":4,"status":"PENDING","reason":"Family function","policyRemarks":"Within policy limit",…}
```
`days` is **4**: Monday to Friday is 5 days, minus the holiday on 2 Oct. An approval task for Priya is created automatically.

### 3.4 Apply CASUAL leave over the quota → REJECTED_BY_POLICY
`POST /api/leave-requests`
```json
{
  "leaveType": "CASUAL",
  "startDate": "2026-11-02",
  "endDate": "2026-11-27",
  "reason": "Long trip"
}
```
→ **201** `{"id":2,"days":20,"status":"REJECTED_BY_POLICY","policyRemarks":"Requested 20 days but only 9 remaining",…}`
The remaining quota is 9 because the 4 pending days from 3.3 are reserved.

### 3.5 Apply SICK leave (cancelled later)
`POST /api/leave-requests`
```json
{
  "leaveType": "SICK",
  "startDate": "2026-10-12",
  "endDate": "2026-10-12",
  "reason": "Doctor visit"
}
```
→ **201** `{"id":3,"days":1,"status":"PENDING",…}`

### 3.6 Apply EARNED leave (rejected later by the manager)
`POST /api/leave-requests`
```json
{
  "leaveType": "EARNED",
  "startDate": "2026-10-19",
  "endDate": "2026-10-20",
  "reason": "Personal work"
}
```
→ **201** `{"id":4,"days":2,"status":"PENDING",…}`

### Error cases

| Step | Body sent to `POST /api/leave-requests` | Expected |
|---|---|---|
| 3.7 | `{"leaveType":"SICK","startDate":"2026-10-10","endDate":"2026-10-11"}` | **400** `"The selected dates fall entirely on weekends or holidays"` |
| 3.8 | `{"leaveType":"SICK","startDate":"2026-09-29","endDate":"2026-09-29"}` | **409** `"You already have pending or approved leave between 2026-09-29 and 2026-09-29"` |
| 3.9 | `{"leaveType":"SICK","startDate":"2026-10-15","endDate":"2026-10-14"}` | **400** `"endDate must not be before startDate"` |
| 3.10 | `{"leaveType":"VACATION","startDate":"2026-10-15","endDate":"2026-10-15"}` | **400** `fieldErrors.leaveType = "leaveType must be CASUAL, SICK or EARNED"` |

More error cases you can try: `"startDate":"2026-12-30","endDate":"2027-01-02"` (different years → 400), and `"startDate":"15-10-2026"` (wrong date format → 400).

### View and cancel

| Step | Request | Expected |
|---|---|---|
| 3.11 | `GET /api/leave-requests/my` | **200**: `content` has 4 requests, newest start date first, plus `page` info |
| 3.12 | `GET /api/leave-requests/1` | **200**: request 1, `PENDING` |
| 3.13 | `PUT /api/leave-requests/3/cancel` | **200** `"status":"CANCELLED"` (the SICK request). Priya's task for it also becomes CANCELLED. |
| 3.14 | `PUT /api/leave-requests/3/cancel` | **409** `"Leave request 3 is CANCELLED and cannot be cancelled"` |
| 3.15 | `GET /api/leave-requests/balance?year=2026` | **200**: CASUAL `pending 4, remaining 9`; EARNED `pending 2, remaining 18`; SICK `remaining 8` |
| 3.16 | `GET /api/approvals/pending` | **403** `"Your role is not allowed to do this"` (employees cannot approve) |
| 3.17 | `GET /api/employees` | **403** (employees cannot list staff) |
| 3.18 | `GET /api/notifications` | **200**: "Your … leave request #… was submitted…", "…rejected by policy…" |

---

## Phase 4 — Teammate Sita: privacy checks

**Login: `sita@company.com` / `Sita@1234`.**

| Step | Request | Expected |
|---|---|---|
| 4.1 | `GET /api/leave-requests/1` | **403** `"You can only view leave of yourself and your direct reports"` |
| 4.2 | `PUT /api/leave-requests/1/cancel` | **403** `"Only the employee who applied (or HR) can cancel this leave"` |

---

## Phase 5 — Manager Priya: approve and reject

**Login: `priya@company.com` / `Priya@123`** (except 5.8).

| Step | Request | Expected |
|---|---|---|
| 5.1 | `GET /api/notifications?unreadOnly=true` | **200**: newest first, for example "Ravi Kumar Singh cancelled SICK leave request #3 …", "Ravi Kumar Singh applied for EARNED leave request #4 … Please approve or reject it." |
| 5.2 | `GET /api/approvals/pending` | **200**: 2 tasks: `{"id":1,"leaveRequestId":1,"status":"PENDING"}` and `{"id":3,"leaveRequestId":4,"status":"PENDING"}` |
| 5.3 | `GET /api/approvals/2` | **200** `{"id":2,"leaveRequestId":3,"status":"CANCELLED",…}` (the cancelled SICK request) |
| 5.4 | `GET /api/employees` | **200**: only her team (Ravi, Sita, Arjun) |
| 5.5 | `GET /api/leave-requests/employee/3` | **200**: Ravi's 4 requests |
| 5.6 | `GET /api/leave-requests/balance/3?year=2026` | **200**: same numbers as step 3.15 |
| 5.7 | `PUT /api/approvals/1/decide?decision=MAYBE` | **400** `"decision must be APPROVED or REJECTED"` |
| 5.8 | `PUT /api/approvals/1/decide?decision=APPROVED` — **login as Arjun** (`arjun@company.com` / `Arjun@123`) | **403** `"Task 1 can only be decided by its approver (employee 2)"` |
| 5.9 | `PUT /api/approvals/1/decide?decision=APPROVED&comments=Enjoy your time` | **200** `{"id":1,"status":"APPROVED","comments":"Enjoy your time","decidedAt":"…"}` |
| 5.10 | `PUT /api/approvals/3/decide?decision=REJECTED&comments=Project deadline` | **200** `{"id":3,"status":"REJECTED",…}` |
| 5.11 | `PUT /api/approvals/1/decide?decision=REJECTED` | **409** `"Task 1 is already APPROVED"` |
| 5.12 | `PUT /api/notifications/1/read` | **200** `{"id":1,…,"read":true}` |
| 5.13 | `PUT /api/notifications/read-all` | **200** `{"updated":3}` |

> In Postman, put the query values in the **Params** tab (`decision`, `comments`). Postman encodes the spaces for you.

---

## Phase 6 — Employee Ravi: after the decisions

**Login: `ravi@company.com` / `Ravi@1234`** (step 6.9 uses the new password).

| Step | Request | Expected |
|---|---|---|
| 6.1 | `GET /api/notifications` | **200**: newest: "Your EARNED leave request #4 … was REJECTED. Comments: Project deadline", then "Your CASUAL leave request #1 … was APPROVED. Comments: Enjoy your time" |
| 6.2 | `GET /api/leave-requests/1` | **200** `"status":"APPROVED"` |
| 6.3 | `GET /api/leave-requests/balance?year=2026` | **200**: CASUAL `used 4, pending 0, remaining 9`; EARNED `pending 0, remaining 20` (the rejected days were given back) |
| 6.4 | `GET /api/leave-requests/summary/3?year=2026` | **200** `{"rejectedCount":2,"unapprovedCount":0}` (1 rejected by policy + 1 rejected by the manager) |
| 6.5 | `PUT /api/leave-requests/1/cancel` | **200** `"status":"CANCELLED"` (approved leave can be cancelled because it hasn't started yet) |
| 6.6 | `GET /api/leave-requests/balance?year=2026` | **200**: CASUAL `used 0, remaining 13` (days given back) |
| 6.7 | `PUT /api/employees/me/password` with body `{"currentPassword":"Ravi@1234","newPassword":"Ravi@5678"}` | **204** (no body) |
| 6.8 | `GET /api/employees/me` (old password `Ravi@1234`) | **401** |
| 6.9 | `GET /api/employees/me` (new password `Ravi@5678`) | **200** |

---

## Phase 7 — HR: oversight and final checks

**Login: HR admin** (7.4 as Sita, 7.7 with no login).

| Step | Request | Expected |
|---|---|---|
| 7.1 | `GET /api/approvals/1` | **200** (HR can view any task) |
| 7.2 | `GET /api/leave-requests/employee/3` | **200** (HR can view anyone's leave) |
| 7.3 | `DELETE /api/employees/4` | **204** (Sita is deactivated) |
| 7.4 | `GET /api/employees/me` as `sita@company.com` / `Sita@1234` | **401** (deactivated accounts cannot log in) |
| 7.5 | `GET /api/nothing-here` | **404** JSON error |
| 7.6 | `DELETE /api/leave-requests/my` | **405** `"Request method 'DELETE' is not supported"` |
| 7.7 | `GET /v3/api-docs` (no login) | **200**: OpenAPI JSON (used by Swagger UI) |

---

## Phase 8 - HR: yearly bonus calculation

**Login: HR admin.** Set the Postman collection variable `bonusYear` to the year you want to calculate.

| Step | Request | Expected |
|---|---|---|
| 8.1 | `GET /api/bonuses/{{employeeId}}?year={{bonusYear}}` | **200** with leave counts, reimbursement counts and amounts, formula components, and `bonusAmount` |

The endpoint includes `appliedLeaveCount`, `approvedLeaveCount`, `approvedLeaveDays`, `rejectedLeaveCount`, `pendingLeaveCount`, `cancelledLeaveCount`, reimbursement counts and amounts by status, and the final calculated bonus. The employee, their direct manager, or HR may view the report.

---

## Check the data in MySQL Workbench

After the run, refresh `leave_management_db` and run:
```sql
USE leave_management_db;
SELECT id, name, email, role, manager_id, active FROM employees;
SELECT id, employee_id, leave_type, start_date, end_date, days, status, policy_remarks FROM leave_requests;
SELECT id, leave_request_id, approver_id, status, comments, decided_at FROM approval_tasks;
SELECT id, recipient_id, is_read, message FROM notifications ORDER BY id;
SELECT id, employee_id, amount, status, created_at, updated_at FROM reimbursements ORDER BY id;
SELECT * FROM holidays;
```
You should see:
- 5 employees, with Sita `active = 0`.
- 4 leave requests: `CANCELLED`, `REJECTED_BY_POLICY`, `CANCELLED`, `REJECTED`.
- 3 approval tasks: `APPROVED`, `CANCELLED`, `REJECTED`.
- 11 notifications.
- 1 holiday.

---

## Quick reference: every endpoint

| # | Method | URL | Role | Step |
|---|---|---|---|---|
| 1 | GET | `/actuator/health` | public | 0.1 |
| 2 | GET | `/v3/api-docs`, `/swagger-ui.html` | public | 7.7 |
| 3 | GET | `/api/employees/me` | any | 1.1, 3.1 |
| 4 | PUT | `/api/employees/me/password` | any | 6.7 |
| 5 | GET | `/api/employees?page&size&sort` | HR, MANAGER | 1.6, 5.4 |
| 6 | GET | `/api/employees/{id}` | self, manager, HR | 1.7 |
| 7 | POST | `/api/employees` | HR | 1.2–1.5 |
| 8 | PUT | `/api/employees/{id}` | HR | 1.8 |
| 9 | DELETE | `/api/employees/{id}` | HR | 7.3 |
| 10 | GET | `/api/policies` | any | 2.1 |
| 11 | POST | `/api/policies` | HR | 2.5 |
| 12 | PUT | `/api/policies/{id}` | HR | 2.3 |
| 13 | DELETE | `/api/policies/{id}` | HR | 2.4 |
| 14 | POST | `/api/policies/evaluate` | any | 2.6 |
| 15 | GET | `/api/holidays?year` | any | 2.11 |
| 16 | POST | `/api/holidays` | HR | 2.8 |
| 17 | DELETE | `/api/holidays/{id}` | HR | 2.12 |
| 18 | POST | `/api/leave-requests` | any | 3.3–3.6 |
| 19 | GET | `/api/leave-requests/my` | any | 3.11 |
| 20 | GET | `/api/leave-requests/{id}` | owner, manager, HR | 3.12 |
| 21 | PUT | `/api/leave-requests/{id}/cancel` | owner, HR | 3.13, 6.5 |
| 22 | GET | `/api/leave-requests/employee/{id}` | self, manager, HR | 5.5, 7.2 |
| 23 | GET | `/api/leave-requests/balance?year` | any | 3.2 |
| 24 | GET | `/api/leave-requests/balance/{id}?year` | self, manager, HR | 5.6 |
| 25 | GET | `/api/leave-requests/summary/{id}?year` | self, manager, HR | 6.4 |
| 26 | GET | `/api/approvals/pending` | MANAGER, HR | 5.2 |
| 27 | GET | `/api/approvals/{id}` | approver, HR | 5.3, 7.1 |
| 28 | PUT | `/api/approvals/{id}/decide?decision&comments` | assigned approver | 5.9, 5.10 |
| 29 | GET | `/api/notifications?unreadOnly&page&size` | any | 3.18, 5.1 |
| 30 | PUT | `/api/notifications/{id}/read` | owner | 5.12 |
| 31 | PUT | `/api/notifications/read-all` | any | 5.13 |
| 32 | GET | `/api/bonuses/{employeeId}?year` | self, manager, HR | 8.1 |

## Troubleshooting

| You see | Fix |
|---|---|
| **401** on every request | Check the Basic Auth tab: the email is the username. For the admin, check the `adminPassword` variable. |
| **409 duplicate email** in step 1.2 | The database isn't fresh. Reset it (see "Before you start"). |
| IDs in the responses differ from this guide | The database already had data. The collection still works because it saves the real IDs in variables. |
| `Could not send request` | The app isn't running. Start it and check `/actuator/health`. |
| 6.8/6.9 fail on a second run | Ravi's password was changed in the first run. Reset the database before running again. |

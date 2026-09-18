# Leave Management

A Spring Boot application for employee leave: employees apply for leave, the policy checks the quota, managers approve or reject it, and everyone gets notified.

It started as three microservices (`leave-policy-service`, `leave-request-service` and `approval-service`, plus `user-service`). They are now modules of **one application**, with one port and one database.

| Module | Package | URL prefix |
|---|---|---|
| Employees and login | `employee`, `security` | `/api/employees` |
| Leave policies | `policy` | `/api/policies` |
| Company holidays | `holiday` | `/api/holidays` |
| Leave requests, balance, summary | `request` | `/api/leave-requests` |
| Approvals | `approval` | `/api/approvals` |
| Notifications | `notification` | `/api/notifications` |

📐 **Design documentation:** [docs/DESIGN.md](docs/DESIGN.md) covers the agile backlog, use case diagram, process flows, state and sequence diagrams, ER diagram and data dictionary. Rendered images are in [docs/diagrams/](docs/diagrams/).

Modules talk to each other through Spring events (`request/LeaveEvents`). For example, when leave is submitted, the approval module creates the manager's task and the notification module informs people, all in the same database transaction.

---

## 1. Tech stack

- Java 25 (compiled with JDK 27), Spring Boot 4.1.1
- Spring Web MVC, Spring Data JPA, Bean Validation, Spring Security (HTTP Basic)
- MySQL 8 with Flyway migrations; H2 for tests and for running without MySQL
- springdoc (Swagger UI), Spring Boot Actuator
- JUnit 5, Mockito, MockMvc

## 2. Ports and database

| What | Value |
|---|---|
| App | **http://localhost:8080** |
| Swagger UI | http://localhost:8080/swagger-ui.html |
| Health | http://localhost:8080/actuator/health |
| MySQL | `localhost:3306`, database **`leave_management_db`** (one database) |
| MySQL user | **`leave_app`**, with rights only on `leave_management_db` (see `db/create-app-user.sql`) |
| Tables | `employees`, `leave_policies`, `holidays`, `leave_requests`, `approval_tasks`, `notifications`, and Flyway's `flyway_schema_history` |

Tables are created and upgraded by **Flyway** from `src/main/resources/db/migration`. Never change a migration that has already run; add a new `V3__...sql` file instead. Hibernate does not change the schema (`ddl-auto=none`).

## 3. Setup (one time)

1. **MySQL Server 8** must be running on port 3306.
2. **Create the database and app user.** Open `db/create-app-user.sql` in MySQL Workbench, replace `CHANGE_ME` with a password, and run it as `root`. *(Already done on this machine.)*
3. **Settings:** `D:\Java-learn\.env` holds the values the app reads:
   ```
   DB_HOST, DB_PORT, DB_NAME, DB_USERNAME, DB_PASSWORD   # MySQL
   ADMIN_EMAIL, ADMIN_PASSWORD                           # first HR login; ADMIN_PASSWORD is required outside the h2 profile
   ```
   The file is outside the repository and must never be committed.

## 4. Run

- **VS Code:** Run and Debug → **Leave Management (MySQL)** → F5.
- **No MySQL?** Use **Leave Management (H2, no MySQL)**. The data is lost when the app stops.
- **Terminal (PowerShell):**
  ```powershell
  cd D:\Java-learn\leave-management
  Get-Content ..\.env | Where-Object { $_ -match '^\w+=' } | ForEach-Object { $k, $v = $_ -split '=', 2; Set-Item "env:$k" $v }
  .\mvnw.cmd spring-boot:run
  ```

On first start the app creates the tables, the 9 default policies, and the **HR admin** (`ADMIN_EMAIL` / `ADMIN_PASSWORD`). Log in as the admin, create the other employees, and change the admin password.

## 5. Test

```powershell
.\mvnw.cmd test
```
There are 42 tests:
- **Unit tests:** working-day counting, policy and carry-forward maths, manager and loop rules.
- **API tests:** `LeaveManagementApiTests` covers every user story, including roles and error cases, on H2.

**Postman:** follow [docs/API_TESTING_GUIDE.md](docs/API_TESTING_GUIDE.md). It covers all 79 requests, step by step, with the data to send and the expected results. Import `postman/leave-management.postman_collection.json`, set `adminPassword` in the collection variables, and run it on a fresh database.

---

## 6. Security and roles

Every `/api/**` call needs **HTTP Basic** auth with the employee's **email and password**. Only `/actuator/health`, `/actuator/info` and Swagger are public.

| Role | Can do |
|---|---|
| `EMPLOYEE` | Own profile, password, leave (apply, cancel, balance), notifications; read policies and holidays |
| `MANAGER` | Everything an employee can, plus view direct reports and their leave, and approve or reject tasks assigned to them |
| `HR` | Everything, plus create, update and deactivate employees and manage policies and holidays. HR can view any leave but only decides tasks assigned to them. |

- Passwords are stored as BCrypt hashes and never returned.
- Deactivated employees cannot log in.
- The last active HR user cannot be demoted or deactivated.

## 7. Business rules

- **Working days:** Saturdays, Sundays and holidays (`/api/holidays`) are not counted. The count is saved on the request (`days`), so later holiday changes do not alter past requests.
- **Quota:** available = `annualQuota` + carried-forward − approved − **pending**. Pending days are reserved, so two pending requests cannot together exceed the quota.
- **Carry-forward:** unused days from last year, capped at `carryForwardLimit`. There is no carry-forward in the joining year (`joiningDate`).
- **Apply rules:**
  - End date must not be before start date.
  - Start and end must be in the same year.
  - A request can span at most 60 calendar days.
  - It must include at least one working day.
  - The employee must have a manager.
  - It must not overlap pending or approved leave (409).
- **Result:** within quota → `PENDING`, and an approval task is created for the manager. Over quota → `REJECTED_BY_POLICY`, with the reason in `policyRemarks`.
- **Decide:** only the assigned approver may decide, only once. The decision is `APPROVED` or `REJECTED`, and comments can be up to 255 characters.
- **Cancel:** the owner or HR can cancel a `PENDING` request, or an `APPROVED` request that has not started. The approval task becomes `CANCELLED` and the days are released.
- **Managers:** a manager must exist, be active, and have the `MANAGER` or `HR` role. An employee cannot be their own manager, and reporting loops are rejected. A manager who still has active reports or pending approvals cannot be deactivated or demoted.
- **Concurrency:** applications by the same employee are processed one at a time (row lock). Approval tasks are locked while being decided, and `@Version` columns catch lost updates (409).
- **Notifications:**
  - Manager: a new request arrives, or a request is cancelled.
  - Employee: a request is submitted, rejected by policy, approved or rejected.
  - Notifications are stored in the database and also logged.

## 8. API reference

| Method | URL | Who | Purpose |
|---|---|---|---|
| GET | `/api/employees/me` | any | Own profile |
| PUT | `/api/employees/me/password` | any | `{currentPassword, newPassword}` → 204 |
| GET | `/api/employees?page&size&sort` | HR (all), MANAGER (reports) | Paged list |
| GET | `/api/employees/{id}` | self, manager, HR | One employee |
| POST | `/api/employees` | HR | `{name, email, password, grade, role, managerId?, joiningDate?}` → 201 |
| PUT | `/api/employees/{id}` | HR | `{name, grade, role, managerId}` |
| DELETE | `/api/employees/{id}` | HR | Deactivate → 204 |
| GET | `/api/policies` | any | All policies |
| POST | `/api/policies` | HR | `{leaveType, grade, annualQuota, carryForwardLimit}` → 201 |
| PUT | `/api/policies/{id}` | HR | `{annualQuota, carryForwardLimit}` |
| DELETE | `/api/policies/{id}` | HR | → 204 |
| POST | `/api/policies/evaluate` | any | `{grade, leaveType, requestedDays, alreadyUsedDays, carriedForwardDays?}` |
| GET | `/api/holidays?year` | any | Holidays of a year |
| POST | `/api/holidays` | HR | `{date, name}` → 201 |
| DELETE | `/api/holidays/{id}` | HR | → 204 |
| POST | `/api/leave-requests` | any | Apply `{leaveType, startDate, endDate, reason?}` → 201 |
| GET | `/api/leave-requests/my?page&size` | any | Own requests (paged) |
| GET | `/api/leave-requests/{id}` | owner, manager, HR | One request |
| PUT | `/api/leave-requests/{id}/cancel` | owner, HR | Cancel |
| GET | `/api/leave-requests/employee/{id}?page&size` | self, manager, HR | Requests of an employee (paged) |
| GET | `/api/leave-requests/balance?year` | any | Own balance per leave type |
| GET | `/api/leave-requests/balance/{employeeId}?year` | self, manager, HR | Balance of an employee |
| GET | `/api/leave-requests/summary/{employeeId}?year` | self, manager, HR | `{rejectedCount, unapprovedCount}` |
| GET | `/api/approvals/pending` | MANAGER, HR | Own pending tasks |
| GET | `/api/approvals/{id}` | approver, HR | One task |
| PUT | `/api/approvals/{id}/decide?decision&comments` | assigned approver | `APPROVED` or `REJECTED` |
| GET | `/api/notifications?unreadOnly&page&size` | any | Own notifications (newest first) |
| PUT | `/api/notifications/{id}/read` | owner | Mark one read |
| PUT | `/api/notifications/read-all` | any | Mark all read → `{updated}` |

Values:
- `grade`: `L1`, `L2`, `L3`
- `role`: `EMPLOYEE`, `MANAGER`, `HR`
- `leaveType`: `CASUAL`, `SICK`, `EARNED`
- Dates use the format `yyyy-MM-dd`.
- Paged responses look like `{content: [...], page: {size, number, totalElements, totalPages}}`, with a maximum page size of 100.

## 9. Errors

Every error returns `{timestamp, status, error, message}`; validation errors also include `fieldErrors`.

| Code | When |
|---|---|
| 400 | Validation failed, bad JSON or date, bad sort field, business rule broken (dates, no manager, no policy, weekend-only, reporting loop, wrong current password) |
| 401 | Not logged in, wrong password, or account deactivated |
| 403 | Role not allowed, or not your record or task |
| 404 | Record or URL not found |
| 405 | Wrong HTTP method |
| 409 | Duplicate (email, policy, holiday), overlapping leave, already decided or cancelled, manager still has reports or approvals, last HR, concurrent update |
| 500 | Unexpected error (details only in the log) |

---

## 10. Agile backlog

**Epic:** Employee leave management. Each story has automated tests in `LeaveManagementApiTests` (and unit tests where noted).

| ID | Story | Sprint |
|---|---|---|
| US-1 | As **HR**, I manage employees (create, update, deactivate) with a grade, role and manager. | 1 |
| US-2 | As **HR**, I manage leave policies per grade and type; anyone can evaluate a request against them. | 1 |
| US-3 | As **HR**, I maintain company holidays so leave counts only working days. | 2 |
| US-4 | As an **employee**, I apply for leave and it is checked against my quota, including carry-forward and pending days. | 2 |
| US-5 | As a **manager**, I see my pending approvals and approve or reject them. | 2 |
| US-6 | As an **employee or manager**, I see leave balance and yearly summary, and can cancel leave. | 3 |
| US-7 | As the **company**, only logged-in users with the right role can use the API. | 3 |
| US-8 | As a **user**, I get notified about leave I applied for or have to approve. | 3 |
| US-9 | As an **API user**, I get clear JSON errors, paged lists, API docs and a health check. | 3 |

**Definition of done:** `mvnw test` passes, the endpoint is in Swagger and Postman, and its error cases return the codes above.

## 11. Not included yet

- **Other modules** from the wider project (attendance, reimbursement, bonus) need their own specifications. Bonus can use `/api/leave-requests/summary`.
- **Email notifications:** notifications are in-app and logged. An email sender can be added in `NotificationService`, and needs an SMTP server.
- **Login tokens (JWT/OAuth)** instead of HTTP Basic, for a web or mobile frontend.

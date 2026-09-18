# Complete System Flow

This flowchart shows the main runtime paths in the Leave Management application, including startup, authentication, leave, approvals, reimbursements, bonus calculation, notifications, persistence, logging, and errors.

```mermaid
flowchart TD
    Start([Application starts]) --> Config[Load application properties and environment]
    Config --> DBConn[Create Hikari MySQL connection pool]
    DBConn --> Flyway[Flyway validates and applies migrations]
    Flyway --> JPA[Initialize JPA repositories and entities]
    JPA --> Seed[Create first HR account if no active HR exists]
    Seed --> Ready([Application ready])

    Client[Client: Swagger, Postman or UI] --> HTTP[HTTP request]
    HTTP --> RequestLog[RequestLoggingFilter<br/>request ID, user, method, URL, timer]
    RequestLog --> Public{Public endpoint?}
    Public -->|Yes| PublicRoute[Health, info, Swagger or OpenAPI]
    Public -->|No| Auth[HTTP Basic authentication]
    Auth --> AuthOK{Credentials valid<br/>and account active?}
    AuthOK -->|No| AuthError[401 JSON response<br/>security warning log]
    AuthOK -->|Yes| RoleCheck[Resolve employee and role]
    RoleCheck --> Permission{Role and ownership allowed?}
    Permission -->|No| Forbidden[403 JSON response<br/>access warning log]
    Permission -->|Yes| Controller[Feature REST controller<br/>validate request DTO]

    Controller --> Feature{Requested feature}

    Feature --> Employee[Employee management]
    Employee --> EmployeeRules[Validate email, role, grade,<br/>manager and reporting loop]
    EmployeeRules --> EmployeeDB[(employees)]
    EmployeeDB --> EmployeeResponse[Employee response]

    Feature --> Policy[Leave policy management/evaluation]
    Policy --> PolicyRules[Find policy by grade and leave type]
    PolicyRules --> PolicyDB[(leave_policies)]
    PolicyDB --> PolicyResponse[Policy response]

    Feature --> Holiday[Holiday management]
    Holiday --> HolidayDB[(holidays)]
    HolidayDB --> HolidayResponse[Holiday response]

    Feature --> Leave[Leave request]
    Leave --> LeaveValidate[Validate dates, manager,<br/>overlap and working days]
    LeaveValidate --> WorkingDays[Exclude weekends and holidays]
    WorkingDays --> Quota[Calculate quota + carry-forward<br/>minus approved and pending days]
    Quota --> Fits{Within quota?}
    Fits -->|No| PolicyRejected[Save REJECTED_BY_POLICY<br/>with policy reason]
    Fits -->|Yes| Pending[Save PENDING leave request]
    Pending --> ApprovalTask[Create manager approval task]
    Pending --> LeaveSubmitted[Publish Submitted event]
    PolicyRejected --> RejectedEvent[Publish RejectedByPolicy event]
    Leave --> LeaveRead[Read balance, summary,<br/>history or cancel request]
    LeaveRead --> LeaveDB[(leave_requests)]

    LeaveSubmitted --> ApprovalEvent[Approval module creates task]
    LeaveSubmitted --> NotifySubmit[Notification module notifies<br/>employee and manager]
    RejectedEvent --> NotifyReject[Notification module notifies employee]
    ApprovalTask --> ApprovalDB[(approval_tasks)]
    NotifySubmit --> NotificationDB[(notifications)]
    NotifyReject --> NotificationDB

    Feature --> Approval[Approval decision]
    Approval --> ApprovalRules[Lock task; verify assigned approver<br/>and task is still PENDING]
    ApprovalRules --> Decision{Decision}
    Decision -->|APPROVED| Approved[Update leave to APPROVED]
    Decision -->|REJECTED| Rejected[Update leave to REJECTED]
    Approved --> ApprovalUpdate[Update approval task and decision time]
    Rejected --> ApprovalUpdate
    ApprovalUpdate --> ApprovalDB
    ApprovalUpdate --> DecisionEvent[Publish decision event]
    DecisionEvent --> LeaveDB
    DecisionEvent --> NotifyDecision[Notify employee]
    NotifyDecision --> NotificationDB

    Feature --> Reimbursement[Reimbursement request]
    Reimbursement --> ReimbursementCreate[Employee submits travel and amount]
    ReimbursementCreate --> ReimbursementDB[(reimbursements)]
    ReimbursementDB --> ReimbursementView[Employee, manager or HR views request]
    ReimbursementView --> ReimbursementDecision{HR decision?}
    ReimbursementDecision -->|APPROVED| ReimbursementApproved[Set APPROVED]
    ReimbursementDecision -->|REJECTED| ReimbursementRejected[Set REJECTED]
    ReimbursementApproved --> ReimbursementDB
    ReimbursementRejected --> ReimbursementDB

    Feature --> Bonus[Yearly bonus report]
    Bonus --> BonusAccess[Verify employee, direct manager or HR]
    BonusAccess --> LeaveMetrics[Aggregate leave requests for selected year]
    BonusAccess --> ReimbursementMetrics[Aggregate reimbursements by submission year]
    LeaveMetrics --> Formula[Apply configurable bonus formula]
    ReimbursementMetrics --> Formula
    Formula --> BonusResponse[Return metrics, penalties,<br/>rewards and final bonus amount]

    EmployeeDB --> Formula
    LeaveDB --> LeaveMetrics
    ReimbursementDB --> ReimbursementMetrics

    Controller --> Transaction[Transactional service operation]
    Transaction --> Feature

    PublicRoute --> Success[2xx response]
    EmployeeResponse --> Success
    PolicyResponse --> Success
    HolidayResponse --> Success
    LeaveDB --> Success
    ApprovalDB --> Success
    ReimbursementView --> Success
    BonusResponse --> Success
    Success --> ResponseLog[Log INFO with request ID,<br/>status and duration]
    AuthError --> ResponseLog
    Forbidden --> ResponseLog
    Controller --> Exception{Unexpected or business exception?}
    Exception -->|Business/client error| ClientError[GlobalExceptionHandler<br/>400/404/409 JSON response]
    Exception -->|Unexpected error| ServerError[GlobalExceptionHandler<br/>500 JSON response]
    ClientError --> ResponseLog
    ServerError --> ResponseLog
    ResponseLog --> FileLog[(logs/leave-management.log<br/>rolling file)]
    ResponseLog --> Response[HTTP response with X-Request-Id]
```

## Main API paths

| Flow | Primary endpoints |
|---|---|
| Health and documentation | `/actuator/health`, `/actuator/info`, `/swagger-ui.html`, `/v3/api-docs` |
| Employees | `/api/employees` |
| Policies and holidays | `/api/policies`, `/api/holidays` |
| Leave lifecycle | `/api/leave-requests`, `/api/approvals` |
| Notifications | `/api/notifications` |
| Reimbursements | `/api/reimbursements` |
| Bonus calculation | `/api/bonuses/{employeeId}?year=YYYY` |

## Business outcomes

- Valid leave within quota becomes `PENDING` and creates a manager approval task.
- Leave outside quota becomes `REJECTED_BY_POLICY`.
- The assigned manager changes pending leave to `APPROVED` or `REJECTED`.
- HR changes pending reimbursements to `APPROVED` or `REJECTED`.
- The bonus report combines yearly leave status metrics and reimbursement status/amount metrics.
- Every request receives an `X-Request-Id`; successful requests, warnings, errors, status codes and timings are written to the rolling log file.

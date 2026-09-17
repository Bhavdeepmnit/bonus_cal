-- Leave management schema (works on MySQL 8 and on H2 in MySQL mode)

CREATE TABLE employees (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    name          VARCHAR(100) NOT NULL,
    email         VARCHAR(150) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    grade         VARCHAR(5)   NOT NULL,
    role          VARCHAR(20)  NOT NULL,
    manager_id    BIGINT       NULL,
    joining_date  DATE         NOT NULL,
    active        BOOLEAN      NOT NULL DEFAULT TRUE,
    version       BIGINT       NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT uk_employees_email UNIQUE (email),
    CONSTRAINT fk_employees_manager FOREIGN KEY (manager_id) REFERENCES employees (id)
);

CREATE TABLE leave_policies (
    id                  BIGINT      NOT NULL AUTO_INCREMENT,
    leave_type          VARCHAR(20) NOT NULL,
    grade               VARCHAR(5)  NOT NULL,
    annual_quota        INT         NOT NULL,
    carry_forward_limit INT         NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_leave_policies_grade_type UNIQUE (grade, leave_type)
);

CREATE TABLE holidays (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    holiday_date DATE         NOT NULL,
    name         VARCHAR(100) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_holidays_date UNIQUE (holiday_date)
);

CREATE TABLE leave_requests (
    id             BIGINT       NOT NULL AUTO_INCREMENT,
    employee_id    BIGINT       NOT NULL,
    leave_type     VARCHAR(20)  NOT NULL,
    start_date     DATE         NOT NULL,
    end_date       DATE         NOT NULL,
    days           INT          NOT NULL,
    status         VARCHAR(20)  NOT NULL,
    reason         VARCHAR(255) NULL,
    policy_remarks VARCHAR(255) NULL,
    created_at     DATETIME     NOT NULL,
    version        BIGINT       NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT fk_leave_requests_employee FOREIGN KEY (employee_id) REFERENCES employees (id)
);
CREATE INDEX idx_leave_requests_employee_start ON leave_requests (employee_id, start_date);

CREATE TABLE approval_tasks (
    id               BIGINT       NOT NULL AUTO_INCREMENT,
    leave_request_id BIGINT       NOT NULL,
    approver_id      BIGINT       NOT NULL,
    status           VARCHAR(20)  NOT NULL,
    comments         VARCHAR(255) NULL,
    decided_at       DATETIME     NULL,
    version          BIGINT       NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT uk_approval_tasks_leave_request UNIQUE (leave_request_id),
    CONSTRAINT fk_approval_tasks_leave_request FOREIGN KEY (leave_request_id) REFERENCES leave_requests (id),
    CONSTRAINT fk_approval_tasks_approver FOREIGN KEY (approver_id) REFERENCES employees (id)
);
CREATE INDEX idx_approval_tasks_approver_status ON approval_tasks (approver_id, status);

CREATE TABLE notifications (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    recipient_id BIGINT       NOT NULL,
    message      VARCHAR(500) NOT NULL,
    is_read      BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at   DATETIME     NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_notifications_recipient FOREIGN KEY (recipient_id) REFERENCES employees (id)
);
CREATE INDEX idx_notifications_recipient ON notifications (recipient_id, is_read);

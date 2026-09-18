CREATE TABLE reimbursements (
    id            BIGINT        NOT NULL AUTO_INCREMENT,
    employee_id   BIGINT        NOT NULL,
    employee_name VARCHAR(100)  NOT NULL,
    travel_type   VARCHAR(50)   NOT NULL,
    source        VARCHAR(100)  NOT NULL,
    destination   VARCHAR(100)  NOT NULL,
    amount        DECIMAL(12,2) NOT NULL,
    status        VARCHAR(20)   NOT NULL,
    created_at    DATETIME      NOT NULL,
    updated_at    DATETIME      NOT NULL,
    version       BIGINT        NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT fk_reimbursements_employee FOREIGN KEY (employee_id) REFERENCES employees (id)
);
CREATE INDEX idx_reimbursements_employee_status ON reimbursements (employee_id, status);

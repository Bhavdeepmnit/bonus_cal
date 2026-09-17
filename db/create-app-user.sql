-- Run once in MySQL Workbench as root.
-- Creates the database and a dedicated user that can only access this database.
-- Replace CHANGE_ME with a strong password and put the same value in D:\Java-learn\.env (DB_PASSWORD).

CREATE DATABASE IF NOT EXISTS leave_management_db;

CREATE USER IF NOT EXISTS 'leave_app'@'localhost' IDENTIFIED BY 'CHANGE_ME';

-- Flyway needs CREATE/ALTER/INDEX/REFERENCES to manage tables; the app needs the DML rights.
GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, ALTER, DROP, INDEX, REFERENCES
    ON leave_management_db.* TO 'leave_app'@'localhost';

FLUSH PRIVILEGES;

-- Default leave policies per grade
INSERT INTO leave_policies (leave_type, grade, annual_quota, carry_forward_limit) VALUES
('CASUAL', 'L1', 10, 3), ('SICK', 'L1', 8, 0), ('EARNED', 'L1', 15, 5),
('CASUAL', 'L2', 12, 4), ('SICK', 'L2', 10, 0), ('EARNED', 'L2', 18, 6),
('CASUAL', 'L3', 15, 5), ('SICK', 'L3', 12, 0), ('EARNED', 'L3', 21, 8);

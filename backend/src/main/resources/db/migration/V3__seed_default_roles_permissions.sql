INSERT INTO roles (
    id,
    code,
    name,
    description,
    system_role
)
VALUES
    (
        '10000000-0000-0000-0000-000000000001',
        'ADMIN',
        'Administrator',
        'Full platform administration role',
        TRUE
    ),
    (
        '10000000-0000-0000-0000-000000000002',
        'MANAGER',
        'Manager',
        'Team, task and approval management role',
        TRUE
    ),
    (
        '10000000-0000-0000-0000-000000000003',
        'EMPLOYEE',
        'Employee',
        'Standard employee access role',
        TRUE
    );


-- =============================================================
-- Default permissions
-- =============================================================

INSERT INTO permissions (
    id,
    code,
    name,
    description
)
VALUES
    ('20000000-0000-0000-0000-000000000001', 'USER_VIEW', 'View Users', 'View user accounts'),
    ('20000000-0000-0000-0000-000000000002', 'USER_CREATE', 'Create Users', 'Create user accounts'),
    ('20000000-0000-0000-0000-000000000003', 'USER_UPDATE', 'Update Users', 'Update user accounts'),
    ('20000000-0000-0000-0000-000000000004', 'ROLE_VIEW', 'View Roles', 'View roles and permissions'),
    ('20000000-0000-0000-0000-000000000005', 'ROLE_ASSIGN', 'Assign Roles', 'Assign roles to users'),
    ('20000000-0000-0000-0000-000000000006', 'EMPLOYEE_VIEW', 'View Employees', 'View employee records'),
    ('20000000-0000-0000-0000-000000000007', 'EMPLOYEE_CREATE', 'Create Employees', 'Create employee records'),
    ('20000000-0000-0000-0000-000000000008', 'EMPLOYEE_UPDATE', 'Update Employees', 'Update employee records'),
    ('20000000-0000-0000-0000-000000000009', 'TEAM_VIEW', 'View Team', 'View assigned team members'),
    ('20000000-0000-0000-0000-000000000010', 'TASK_VIEW', 'View Tasks', 'View authorized tasks'),
    ('20000000-0000-0000-0000-000000000011', 'TASK_CREATE', 'Create Tasks', 'Create project tasks'),
    ('20000000-0000-0000-0000-000000000012', 'TASK_ASSIGN', 'Assign Tasks', 'Assign tasks to employees'),
    ('20000000-0000-0000-0000-000000000013', 'TASK_UPDATE', 'Update Tasks', 'Update authorized tasks'),
    ('20000000-0000-0000-0000-000000000014', 'TIMESHEET_SUBMIT', 'Submit Timesheets', 'Submit personal timesheets'),
    ('20000000-0000-0000-0000-000000000015', 'TIMESHEET_APPROVE', 'Approve Timesheets', 'Approve team timesheets'),
    ('20000000-0000-0000-0000-000000000016', 'LEAVE_APPLY', 'Apply Leave', 'Submit personal leave requests'),
    ('20000000-0000-0000-0000-000000000017', 'LEAVE_APPROVE', 'Approve Leave', 'Approve team leave requests'),
    ('20000000-0000-0000-0000-000000000018', 'REPORT_VIEW', 'View Reports', 'View authorized reports'),
    ('20000000-0000-0000-0000-000000000019', 'AUDIT_VIEW', 'View Audit Logs', 'View security and business audit events'),
    ('20000000-0000-0000-0000-000000000020', 'NOTIFICATION_VIEW', 'View Notifications', 'View personal notifications'),
    ('20000000-0000-0000-0000-000000000021', 'CHAT_USE', 'Use Chat', 'Use authorized private and group conversations');


-- =============================================================
-- ADMIN permissions
-- =============================================================

INSERT INTO role_permissions (role_id, permission_id)
SELECT
    '10000000-0000-0000-0000-000000000001'::UUID,
    id
FROM permissions;


-- =============================================================
-- MANAGER permissions
-- =============================================================

INSERT INTO role_permissions (role_id, permission_id)
SELECT
    '10000000-0000-0000-0000-000000000002'::UUID,
    id
FROM permissions
WHERE code IN (
    'USER_VIEW',
    'EMPLOYEE_VIEW',
    'TEAM_VIEW',
    'TASK_VIEW',
    'TASK_CREATE',
    'TASK_ASSIGN',
    'TASK_UPDATE',
    'TIMESHEET_SUBMIT',
    'TIMESHEET_APPROVE',
    'LEAVE_APPLY',
    'LEAVE_APPROVE',
    'REPORT_VIEW',
    'NOTIFICATION_VIEW',
    'CHAT_USE'
);


-- =============================================================
-- EMPLOYEE permissions
-- =============================================================

INSERT INTO role_permissions (role_id, permission_id)
SELECT
    '10000000-0000-0000-0000-000000000003'::UUID,
    id
FROM permissions
WHERE code IN (
    'TASK_VIEW',
    'TASK_UPDATE',
    'TIMESHEET_SUBMIT',
    'LEAVE_APPLY',
    'NOTIFICATION_VIEW',
    'CHAT_USE'
);
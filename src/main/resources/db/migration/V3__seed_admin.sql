INSERT IGNORE INTO users (email, full_name, password, role, active)
VALUES (
    'admin@finance.com',
    'System Admin',
    '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
    'ADMIN',
    TRUE
);

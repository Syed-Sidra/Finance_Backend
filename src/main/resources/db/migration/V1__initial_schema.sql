CREATE TABLE IF NOT EXISTS users (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    email      VARCHAR(100) NOT NULL UNIQUE,
    full_name  VARCHAR(100) NOT NULL,
    password   VARCHAR(255) NOT NULL,
    role       ENUM('VIEWER','ANALYST','ADMIN') NOT NULL DEFAULT 'VIEWER',
    active     BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_users_email (email),
    INDEX idx_users_role  (role)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS transactions (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    amount       DECIMAL(15,2) NOT NULL,
    type         ENUM('INCOME','EXPENSE') NOT NULL,
    category     VARCHAR(100) NOT NULL,
    date         DATE NOT NULL,
    notes        VARCHAR(500),
    deleted      BOOLEAN NOT NULL DEFAULT FALSE,
    created_by   BIGINT NOT NULL,
    created_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_transactions_user FOREIGN KEY (created_by) REFERENCES users(id),
    INDEX idx_tx_type    (type),
    INDEX idx_tx_date    (date),
    INDEX idx_tx_deleted (deleted),
    INDEX idx_tx_category(category)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

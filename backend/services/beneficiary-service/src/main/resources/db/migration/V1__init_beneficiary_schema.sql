-- Beneficiary Service DB Schema

CREATE TABLE IF NOT EXISTS beneficiaries (
    id CHAR(36) NOT NULL DEFAULT (UUID()),
    user_id CHAR(36) NOT NULL,
    nickname VARCHAR(100) NOT NULL,
    account_number VARCHAR(30) NOT NULL,
    account_holder_name VARCHAR(200) NOT NULL,
    bank_name VARCHAR(100),
    bank_code VARCHAR(20),
    currency VARCHAR(3) NOT NULL DEFAULT 'USD',
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uq_beneficiary_user_account (user_id, account_number)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_beneficiaries_user_id ON beneficiaries(user_id);

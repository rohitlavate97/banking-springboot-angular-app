-- Fraud Service DB Schema

CREATE TABLE IF NOT EXISTS fraud_alerts (
    id CHAR(36) NOT NULL DEFAULT (UUID()),
    user_id CHAR(36) NOT NULL,
    transaction_reference VARCHAR(50) NOT NULL,
    account_number VARCHAR(30) NOT NULL,
    transaction_amount DECIMAL(19,4) NOT NULL,
    rule_triggered VARCHAR(50) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'OPEN',
    description VARCHAR(500) NOT NULL,
    reviewed_by VARCHAR(100),
    reviewed_at DATETIME,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_fraud_alerts_user_id ON fraud_alerts(user_id);
CREATE INDEX idx_fraud_alerts_transaction_reference ON fraud_alerts(transaction_reference);
CREATE INDEX idx_fraud_alerts_status ON fraud_alerts(status);
CREATE INDEX idx_fraud_alerts_created_at ON fraud_alerts(created_at);

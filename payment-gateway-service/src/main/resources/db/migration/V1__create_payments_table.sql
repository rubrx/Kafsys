CREATE TABLE payments (
    id                     VARCHAR(36)    NOT NULL PRIMARY KEY,
    transaction_id         VARCHAR(36)    NOT NULL UNIQUE,
    source_account_id      VARCHAR(36)    NOT NULL,
    destination_account_id VARCHAR(36)    NOT NULL,
    amount                 NUMERIC(19, 4) NOT NULL,
    currency               CHAR(3)        NOT NULL,
    status                 VARCHAR(20)    NOT NULL DEFAULT 'PROCESSING',
    gateway_reference      VARCHAR(100),
    failure_reason         VARCHAR(255),
    processed_at           TIMESTAMP      NOT NULL DEFAULT NOW(),
    settled_at             TIMESTAMP
);

CREATE INDEX idx_payments_transaction_id ON payments (transaction_id);
CREATE INDEX idx_payments_status         ON payments (status);
CREATE INDEX idx_payments_processed_at   ON payments (processed_at DESC);

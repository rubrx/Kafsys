CREATE TABLE alerts (
    id             VARCHAR(36)  NOT NULL PRIMARY KEY,
    account_id     VARCHAR(36)  NOT NULL,
    transaction_id VARCHAR(36),
    type           VARCHAR(40)  NOT NULL,
    message        VARCHAR(500) NOT NULL,
    read           BOOLEAN      NOT NULL DEFAULT FALSE,
    triggered_at   TIMESTAMP    NOT NULL,
    read_at        TIMESTAMP
);

CREATE INDEX idx_alerts_account_id     ON alerts (account_id);
CREATE INDEX idx_alerts_transaction_id ON alerts (transaction_id);
CREATE INDEX idx_alerts_type           ON alerts (type);
CREATE INDEX idx_alerts_triggered_at   ON alerts (triggered_at DESC);
CREATE INDEX idx_alerts_unread         ON alerts (account_id, read) WHERE read = FALSE;

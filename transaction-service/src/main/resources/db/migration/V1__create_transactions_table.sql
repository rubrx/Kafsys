CREATE TABLE transactions (
    id                     VARCHAR(36)    NOT NULL PRIMARY KEY,
    source_account_id      VARCHAR(36)    NOT NULL,
    destination_account_id VARCHAR(36)    NOT NULL,
    amount                 NUMERIC(19, 4) NOT NULL,
    currency               CHAR(3)        NOT NULL,
    type                   VARCHAR(20)    NOT NULL,
    status                 VARCHAR(30)    NOT NULL DEFAULT 'INITIATED',
    reference_note         VARCHAR(255),
    rejection_source       VARCHAR(50),
    initiated_at           TIMESTAMP      NOT NULL DEFAULT NOW(),
    completed_at           TIMESTAMP,
    version                BIGINT         NOT NULL DEFAULT 0
);

CREATE INDEX idx_tx_source_account  ON transactions (source_account_id);
CREATE INDEX idx_tx_dest_account    ON transactions (destination_account_id);
CREATE INDEX idx_tx_status          ON transactions (status);
CREATE INDEX idx_tx_initiated_at    ON transactions (initiated_at DESC);

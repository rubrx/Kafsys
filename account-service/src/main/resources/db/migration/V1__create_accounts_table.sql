CREATE TABLE accounts (
    id               VARCHAR(36)    NOT NULL PRIMARY KEY,
    account_number   VARCHAR(20)    NOT NULL UNIQUE,
    owner_id         VARCHAR(36)    NOT NULL,
    owner_name       VARCHAR(100)   NOT NULL,
    balance          NUMERIC(19, 4) NOT NULL DEFAULT 0,
    reserved_balance NUMERIC(19, 4) NOT NULL DEFAULT 0,
    currency         CHAR(3)        NOT NULL,
    status           VARCHAR(20)    NOT NULL DEFAULT 'PENDING_KYC',
    kyc_status       VARCHAR(20)    NOT NULL DEFAULT 'NOT_STARTED',
    created_at       TIMESTAMP      NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMP      NOT NULL DEFAULT NOW(),
    version          BIGINT         NOT NULL DEFAULT 0
);

CREATE INDEX idx_accounts_owner_id ON accounts (owner_id);
CREATE INDEX idx_accounts_number   ON accounts (account_number);
CREATE INDEX idx_accounts_status   ON accounts (status);

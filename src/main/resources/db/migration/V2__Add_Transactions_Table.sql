CREATE TABLE transactions(
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    source_iban VARCHAR(24),
    target_iban VARCHAR(24),
    amount NUMERIC(15,2) NOT NULL DEFAULT 0.0,
    type  VARCHAR(25),
    timestamp TIMESTAMP NOT NULL
);
CREATE INDEX idx_transactions_source_iban ON transactions(source_iban);
CREATE INDEX idx_transactions_target_iban ON transactions(target_iban);
CREATE INDEX idx_transactions_timestamp ON transactions(timestamp DESC);
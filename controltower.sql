-- 1. Ingestion Batches
CREATE TABLE source_batch (
    batch_id VARCHAR(64) PRIMARY KEY,
    source_system VARCHAR(32) NOT NULL,
    partner_code VARCHAR(32) NOT NULL,
    declared_count INT NOT NULL,
    declared_amount_paise BIGINT NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 2. Immutable Raw Records (Lineage & Audit)
CREATE TABLE raw_source_record (
    record_id VARCHAR(64) PRIMARY KEY,
    batch_id VARCHAR(64) REFERENCES source_batch(batch_id),
    source_system VARCHAR(32) NOT NULL,
    payload_hash VARCHAR(64) NOT NULL,
    raw_payload TEXT NOT NULL,
    quarantined BOOLEAN DEFAULT FALSE,
    quarantine_reason TEXT,
    received_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_raw_source_hash ON raw_source_record(payload_hash);

-- 3. Normalized Canonical Events
CREATE TABLE canonical_event (
    event_id VARCHAR(64) PRIMARY KEY,
    raw_record_id VARCHAR(64) REFERENCES raw_source_record(record_id),
    business_event_id VARCHAR(64) NOT NULL,
    loan_reference VARCHAR(64) NOT NULL,
    partner_code VARCHAR(32) NOT NULL,
    source_system VARCHAR(32) NOT NULL,
    amount_paise BIGINT NOT NULL,
    currency VARCHAR(3) DEFAULT 'INR',
    event_status VARCHAR(32) NOT NULL,
    source_timestamp TIMESTAMP WITH TIME ZONE NOT NULL,
    received_timestamp TIMESTAMP WITH TIME ZONE NOT NULL
);
CREATE INDEX idx_canonical_loan_ref ON canonical_event(loan_reference);

-- 4. Reconciliation Decisions (Idempotency Guard)
CREATE TABLE match_decision (
    decision_id VARCHAR(64) PRIMARY KEY,
    business_event_id VARCHAR(64) NOT NULL,
    rule_version VARCHAR(16) NOT NULL,
    match_level VARCHAR(32) NOT NULL, -- EXACT, COMPOSITE, TIMING, PROBABLE, UNRESOLVED
    matched_event_ids TEXT[] NOT NULL,
    evidence_payload JSONB NOT NULL,
    decided_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_event_rule_version UNIQUE (business_event_id, rule_version)
);

-- 5. Exception Queue
CREATE TABLE reconciliation_exception (
    exception_id VARCHAR(64) PRIMARY KEY,
    business_event_id VARCHAR(64) NOT NULL,
    classification VARCHAR(64) NOT NULL,
    exposure_amount_paise BIGINT NOT NULL,
    owner_role VARCHAR(64) NOT NULL,
    recommended_action TEXT NOT NULL,
    status VARCHAR(32) DEFAULT 'OPEN',
    override_reason TEXT,
    actor_id VARCHAR(64),
    version BIGINT DEFAULT 0, -- Optimistic lock
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 6. Close Decision Records
CREATE TABLE close_summary (
    close_id VARCHAR(64) PRIMARY KEY,
    batch_id VARCHAR(64) REFERENCES source_batch(batch_id),
    decision VARCHAR(16) NOT NULL, -- CLOSE or HOLD
    total_instructed_paise BIGINT NOT NULL,
    matched_paise BIGINT NOT NULL,
    timing_pending_paise BIGINT NOT NULL,
    exception_paise BIGINT NOT NULL,
    unaccounted_paise BIGINT NOT NULL,
    blocking_exceptions JSONB,
    decided_by VARCHAR(64) NOT NULL,
    decided_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);
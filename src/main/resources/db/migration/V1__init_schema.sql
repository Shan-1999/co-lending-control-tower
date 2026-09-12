-- ============================================================================
-- V1__init_schema.sql — Co-Lending Control Tower Full Schema
-- Financial Invariant: All monetary values stored as BIGINT (Indian Paise)
-- ============================================================================

-- 1. Source Batch: Tracks each ingested file/feed batch
CREATE TABLE source_batch (
    batch_id              VARCHAR(64)  PRIMARY KEY,
    source_system         VARCHAR(32)  NOT NULL CHECK (source_system IN ('ORIGINATOR', 'BANK', 'LMS')),
    partner_code          VARCHAR(32)  NOT NULL,
    business_date         DATE         NOT NULL,
    declared_count        INT          NOT NULL,
    declared_amount_paise BIGINT       NOT NULL,
    status                VARCHAR(32)  NOT NULL DEFAULT 'RECEIVED'
                          CHECK (status IN ('RECEIVED', 'VALIDATED', 'QUARANTINED', 'PROCESSED')),
    received_at           TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_source_batch_partner_date ON source_batch(partner_code, business_date);

-- 2. Raw Source Record: Immutable raw payloads with SHA-256 lineage
CREATE TABLE raw_source_record (
    record_id         VARCHAR(64)  PRIMARY KEY,
    batch_id          VARCHAR(64)  NOT NULL REFERENCES source_batch(batch_id),
    source_system     VARCHAR(32)  NOT NULL,
    payload_hash      VARCHAR(64)  NOT NULL,
    raw_payload       TEXT         NOT NULL,
    quarantined       BOOLEAN      DEFAULT FALSE,
    quarantine_reason TEXT,
    received_at       TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_batch_payload_hash UNIQUE (batch_id, payload_hash)
);

CREATE INDEX idx_raw_source_hash ON raw_source_record(payload_hash);
CREATE INDEX idx_raw_source_batch ON raw_source_record(batch_id);

-- 3. Canonical Event: Normalized, immutable events derived from raw records
CREATE TABLE canonical_event (
    event_id            VARCHAR(64)  PRIMARY KEY,
    raw_record_id       VARCHAR(64)  NOT NULL REFERENCES raw_source_record(record_id),
    business_event_id   VARCHAR(64)  NOT NULL,
    correlation_id      VARCHAR(64)  NOT NULL,
    loan_reference      VARCHAR(64)  NOT NULL,
    partner_code        VARCHAR(32)  NOT NULL,
    source_system       VARCHAR(32)  NOT NULL,
    event_type          VARCHAR(32)  NOT NULL
                        CHECK (event_type IN ('DISBURSEMENT_INSTRUCTION', 'SETTLEMENT_DEBIT', 'LMS_BOOKING', 'REVERSAL')),
    amount_paise        BIGINT       NOT NULL,
    currency            VARCHAR(3)   DEFAULT 'INR',
    source_status       VARCHAR(32)  NOT NULL,
    canonical_status    VARCHAR(32)  NOT NULL
                        CHECK (canonical_status IN ('SUCCESS', 'FAILED', 'PENDING', 'REVERSED')),
    reversal_reference  VARCHAR(64),
    source_timestamp    TIMESTAMP WITH TIME ZONE NOT NULL,
    received_timestamp  TIMESTAMP WITH TIME ZONE NOT NULL,
    cutoff_timestamp    TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_canonical_loan_ref ON canonical_event(loan_reference);
CREATE INDEX idx_canonical_correlation ON canonical_event(correlation_id);
CREATE INDEX idx_canonical_business_event ON canonical_event(business_event_id);
CREATE INDEX idx_canonical_source_system ON canonical_event(source_system);

-- 4. Match Decision: Reconciliation outcomes with idempotency guard
CREATE TABLE match_decision (
    decision_id        VARCHAR(64)    PRIMARY KEY,
    business_event_id  VARCHAR(64)    NOT NULL,
    rule_version       VARCHAR(16)    NOT NULL,
    match_level        VARCHAR(32)    NOT NULL
                       CHECK (match_level IN ('EXACT', 'COMPOSITE', 'TIMING', 'PROBABLE', 'UNRESOLVED')),
    matched_event_ids  TEXT[]         NOT NULL,
    confidence_score   NUMERIC(5,4),
    evidence_payload   JSONB          NOT NULL,
    decided_at         TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_event_rule_version UNIQUE (business_event_id, rule_version)
);

CREATE INDEX idx_match_decision_level ON match_decision(match_level);

-- 5. Reconciliation Exception: Break queue with ownership and SLA tracking
CREATE TABLE reconciliation_exception (
    exception_id          VARCHAR(64)  PRIMARY KEY,
    business_event_id     VARCHAR(64)  NOT NULL,
    classification        VARCHAR(64)  NOT NULL,
    partner_code          VARCHAR(32)  NOT NULL,
    exposure_amount_paise BIGINT       NOT NULL,
    owner_role            VARCHAR(64)  NOT NULL,
    recommended_action    TEXT         NOT NULL,
    priority              VARCHAR(16)  NOT NULL DEFAULT 'MEDIUM'
                          CHECK (priority IN ('CRITICAL', 'HIGH', 'MEDIUM', 'LOW')),
    sla_deadline          TIMESTAMP WITH TIME ZONE NOT NULL,
    status                VARCHAR(32)  DEFAULT 'OPEN'
                          CHECK (status IN ('OPEN', 'IN_PROGRESS', 'RESOLVED', 'OVERRIDDEN')),
    override_reason       TEXT,
    actor_id              VARCHAR(64),
    version               BIGINT       DEFAULT 0,
    created_at            TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_exception_status ON reconciliation_exception(status);
CREATE INDEX idx_exception_priority ON reconciliation_exception(priority);
CREATE INDEX idx_exception_partner ON reconciliation_exception(partner_code);

-- 6. Close Summary: Daily close/hold decisions with full control equation
CREATE TABLE close_summary (
    close_id                  VARCHAR(64)  PRIMARY KEY,
    batch_id                  VARCHAR(64)  NOT NULL REFERENCES source_batch(batch_id),
    business_date             DATE         NOT NULL,
    decision                  VARCHAR(16)  NOT NULL CHECK (decision IN ('CLOSE', 'HOLD')),
    opening_position_paise    BIGINT       NOT NULL,
    valid_movements_paise     BIGINT       NOT NULL,
    reversals_paise           BIGINT       NOT NULL,
    closing_position_paise    BIGINT       NOT NULL,
    matched_paise             BIGINT       NOT NULL,
    timing_pending_paise      BIGINT       NOT NULL,
    unresolved_exception_paise BIGINT      NOT NULL,
    quarantined_paise         BIGINT       NOT NULL,
    unaccounted_paise         BIGINT       NOT NULL,
    blocking_exceptions       JSONB,
    decided_by                VARCHAR(64)  NOT NULL,
    decided_at                TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_close_summary_date ON close_summary(business_date);

-- 7. Audit Log: Append-only immutable audit trail
CREATE TABLE audit_log (
    log_id       VARCHAR(64)  PRIMARY KEY,
    entity_name  VARCHAR(64)  NOT NULL,
    entity_id    VARCHAR(64)  NOT NULL,
    action       VARCHAR(32)  NOT NULL,
    actor_id     VARCHAR(64)  NOT NULL,
    before_state JSONB,
    after_state  JSONB,
    reason       TEXT,
    rule_version VARCHAR(16)  NOT NULL,
    created_at   TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_audit_entity ON audit_log(entity_name, entity_id);
CREATE INDEX idx_audit_actor ON audit_log(actor_id);
CREATE INDEX idx_audit_created ON audit_log(created_at);


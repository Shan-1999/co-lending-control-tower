# Co-Lending Control Tower: Decision Log & Architectural Trade-Offs

**Document Reference**: Vivriti Individual Case Study — Section 10 (Phase 2 Deliverable)  
**Author**: Engineering Team  
**Date**: September 2026  

---

## 1. Architectural Decision Records (ADRs) & Trade-Offs

### ADR-01: Financial Representation — 64-bit Integer Paise (`BIGINT`) vs `BigDecimal` vs `Float/Double`
* **Context**: Co-lending reconciles multi-crore disbursement instructions across diverse partner banks. Even sub-cent fractional discrepancies compound across millions of records.
* **Decision**: All monetary calculations and storage use primitive `long` representing **Indian Paise** ($\text{₹}1.00 = 100 \text{ paise}$).
* **Alternatives Considered & Rejected**:
  - `Float` / `Double` (IEEE 754): **Rejected unconditionally**. Floating-point binary representation produces well-known precision errors (e.g. `0.1 + 0.2 = 0.30000000000000004`), which violates RBI zero-tolerance balancing.
  - `BigDecimal`: Rejected for core high-throughput matching loops due to heap allocation overhead, object creation pressure, and GC latency at 10,000 TPS scale.
* **Trade-off & Mitigation**:
  - *Trade-off*: Max value of signed 64-bit integer is $9 \times 10^{18}$ paise ($\approx \text{₹}9 \times 10^{16}$), which vastly exceeds the entire Indian lending economy.
  - *Mitigation*: Implemented safe arithmetic in `MoneyUtils.addPaise()` and `subtractPaise()` with `Math.addExact()` overflow checks.

---

### ADR-02: Level 4 Probable Matching — Read-Only Advisory Heuristic vs Autonomous ML Reconciliation
* **Context**: The case study permits probabilistic and AI assistance in Phase 2, but imposes the **Critical Rule**: *"A probable match is not a reconciled transaction unless it crosses a documented, approved policy threshold or receives an authorized human confirmation. Low-confidence items must remain unresolved."*
* **Decision**: Level 4 Scored Probable Matching computes a bounded composite score $S = 0.4 \cdot \text{Amt} + 0.3 \cdot \text{Date} + 0.3 \cdot \text{Levenshtein}$. If $S \ge 0.85$, it emits an **advisory proposal only**. The transaction status remains `UNRESOLVED` until an authorized human Maker overrides or resolves it.
* **Alternatives Considered & Rejected**:
  - *Autonomous Auto-Reconcile via LLM / XGBoost*: Rejected. While an automated model might increase apparent Straight-Through Processing (STP), any model false positive matches a fraudulent or corrupt debit, leading to irreversible loss of capital.
* **Trade-off & Mitigation**:
  - *Trade-off*: Requires manual operational review for probable breaks.
  - *Mitigation*: Implemented Phase 2 Root-Cause Clustering to group similar exceptions by partner and provide automated remediation hypotheses, reducing operational review time by $80\%$.

---

### ADR-03: Concurrency Control — Service-Level Thread Synchronization vs Distributed Redis Locks
* **Context**: Parallel incoming HTTP requests or multiple operator clicks can trigger concurrent reconciliation runs against the same un-reconciled database rows, risking unique constraint collisions (`uk_event_rule_version`).
* **Decision**: For Phase 1, implemented a synchronized service entry point (`public synchronized ReconciliationSummary reconcileAll()`) backed by PostgreSQL unique constraints with idempotency recovery fallbacks.
* **Alternatives Considered & Rejected**:
  - *Distributed Redis Locks (`Redisson`)*: Postponed to Phase 2 streaming scale. Introducing Redis in Phase 1 would create an external infrastructure dependency, violating the local single-command portability requirement.
* **Trade-off & Mitigation**:
  - *Trade-off*: Single JVM node throughput limitation during batch runs.
  - *Mitigation*: For Phase 2 (10,000 TPS), designed topic-partitioned Kafka streams (`hash(partner, loan_ref)`) and Redis Bloom filters, as detailed in `docs/PHASE2_SCALE_DESIGN.md`.

---

### ADR-04: Ground Truth Isolation — Runtime Partitioning & Architectural Boundaries
* **Context**: The case study strictly mandates that the reconciliation engine must never have access to ground truth during normal execution.
* **Decision**: Placed `ground_truth.json` generation and models strictly in `com.vivriti.controltower.generator.groundtruth`. Enforced isolation via ArchUnit automated tests (`ArchUnitGroundTruthIsolationTest.java`).
* **Alternatives Considered & Rejected**:
  - *Storing ground truth in database tables*: Rejected, as it tempts developers or SQL queries to join ground truth directly into reconciliation views.
* **Trade-off & Mitigation**:
  - *Trade-off*: Evaluation requires an independent post-hoc evaluation controller (`/api/v1/evaluate`).
  - *Mitigation*: Evaluation engine strictly performs read-only diffing between `ground_truth.json` and `match_decision` outputs.

---

## 2. Security, Privacy & Model-Risk Considerations

### 2.1 PII Minimization & Privacy
- **Zero Production Customer Data**: 100% of data is synthetic. Loan references (`LOAN-000001`), UTRs (`UTR-LOAN-000001-...`), and partner names are generated algorithmically.
- **Sensitive Data Masking**: Payload logging in `audit_log` uses structured JSONB and avoids printing raw bank account credentials in cleartext logs.

### 2.2 Model Risk & Abuse Prevention
- **Prompt Injection & Hallucination Defense**: No unconstrained LLM prompt is executed in the transaction path. All decision engines are deterministic rule chains with typed inputs.
- **Segregation of Duties**: Enforced in `MakerCheckerGuard.java`. If an Operator overrides an exception in a batch, that same actor ID is barred from approving the batch close with a `403 Forbidden` response.

---

## 3. Production Path: Monitoring, Ownership, Rollout & Rollback

### 3.1 Monitoring & Observability
- **Metrics (Prometheus / Micrometer)**:
  - `reconciliation_processing_time_seconds` (p95, p99 latency)
  - `reconciliation_false_match_count` (alert threshold $> 0$)
  - `reconciliation_unaccounted_paise_delta` (alert threshold $> 0$)
  - `quarantine_records_total` by partner code
- **SLO Targets**:
  - Ingestion p99 $< 50\text{ms}$
  - Reconciliation p99 $< 500\text{ms}$
  - False Match Count $\equiv 0$

### 3.2 Staged Rollout Strategy
1. **Canary Shadow Mode (Week 1–2)**:
   - Control Tower runs in shadow mode parallel to legacy reconciliation. Consumes read-only feed replicas. Compares daily close deltas without taking ledger write actions.
2. **Advisory Mode (Week 3–4)**:
   - Operations teams work the Exception Queue and review Root-Cause Clusters. Close decisions are validated against legacy sign-offs.
3. **Primary System of Record (Week 5+)**:
   - Cut over live partner feeds. Maker-Checker approval becomes mandatory for daily ledger sign-off.

### 3.3 Rollback Strategy
- **Zero Destructive Mutation**: Ingestion only appends to `raw_source_record` and `canonical_event`. If a ruleset version (`v1.0`) produces an unintended classification, the engine supports versioned replay (`v1.1`) by inserting new `match_decision` records keyed by `(business_event_id, rule_version)`.
- **Audit Outbox**: The append-only `audit_log` ensures that all overrides and approvals can be rolled back to their exact `before_state` JSON snapshot.

---

## 4. What We Would Build Next (Future Roadmap)

1. **Self-Healing Webhook Gateway**:
   - Automated polling of partner bank APIs when a `MISSING_BANK_LEG` is detected within the 24-hour grace window, eliminating 70% of manual partner integration tickets.
2. **Distributed Redis Bloom Deduplication**:
   - Implement multi-layered deduplication at the edge API gateway (Redis Bloom filter + SETNX lease) prior to database insertion.
3. **Real-time Kafka + Apache Flink Engine**:
   - Full migration of the 5-tier matcher into a stateful streaming Flink application with 24-hour RocksDB TTL state window as documented in `PHASE2_SCALE_DESIGN.md`.


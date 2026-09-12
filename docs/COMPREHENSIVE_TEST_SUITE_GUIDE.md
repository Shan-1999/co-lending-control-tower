# Co-Lending Control Tower: Comprehensive Production Test Suite & Execution Guide

---

## 1. Quality Assurance & Evaluation Framework

The Vivriti Co-Lending Control Tower is designed as a **mission-critical financial infrastructure** component. In accordance with the Vivriti Case Study requirements (Pages 4–13), every financial movement must be traceable, breaks must be owned with enforceable SLAs, and batch closes must balance to zero paise.

### Testing Principles
1. **Deterministic Reproducibility**: Given the same seed and ruleset, the system must produce byte-for-byte identical outputs.
2. **Zero False Matches Tolerance**: False match count must strictly be `0`, with `₹0.00` false exposure. Probable matches are strictly advisory and never auto-reconciled.
3. **Double-Entry Balance Verification**: Control equations must hold with 0 paise unaccounted delta.
4. **Idempotency & Immutability**: Re-feeding or re-executing must yield zero duplicates or state mutations.
5. **Segregation of Duties**: Makers cannot be Checkers.

---

## 2. Test Case Matrix Overview

| Test ID | Test Category | Target Subsystem | Severity / Gate |
| :--- | :--- | :--- | :--- |
| **TC-01** | Database Management | Transactional Storage | Critical |
| **TC-02** | Data Generation | Synthetic Generator | Hard Gate Pass |
| **TC-03** | Data Variation | Determinism & Seed Change | Hard Gate Pass |
| **TC-04** | Multi-Format Ingestion | Adapters (JSON, CSV, Pipe) | Critical |
| **TC-05** | Ingestion Idempotency | SHA-256 Deduplication | Hard Gate Pass |
| **TC-06** | Data Quality & Quarantine| Failure Isolation | Critical |
| **TC-07** | Header Drift Validation | Batch Header Control Totals | Critical |
| **TC-08** | 5-Tier Reconciliation | Core Matcher Engine | Critical |
| **TC-09** | Hard Gate Evaluation | Ground Truth Evaluator | **Mandatory Hard Gate** |
| **TC-10** | Anomaly Coverage | Confusion Matrix (10 Classes) | Mandatory Hard Gate |
| **TC-11** | Exception Management | Priority & SLA Scorer | Critical |
| **TC-12** | Maker Audit Trail | Exception Override | Critical |
| **TC-13** | Root-Cause Synthesis | Phase 2 Clustering | Phase 2 Stretch |
| **TC-14** | Accounting Equations | Balance Sheet Equation 1 | Critical |
| **TC-15** | Zero-Tolerance Gate | 1-Paise Variance Block (HOLD)| **Non-Negotiable** |
| **TC-16** | Security Governance | Maker-Checker Segregation (403)| **Non-Negotiable** |
| **TC-17** | Batch Certification | Checker Close Approval | Critical |
| **TC-18** | Forensic Auditability | Cryptographic Lineage Trace | Critical |
| **TC-19** | High Concurrency | Thread-Safe Serialization | Production Grade |
| **TC-20** | Automated Regression | JUnit 5 & ArchUnit Suite | Hard Gate Pass |

---

## 3. Detailed Test Case Specifications

---

### TC-01: Clean-Slate Database Reset
- **Objective**: Verify that all 7 transactional tables can be truncated cleanly without foreign key lockups, restoring the system to a clean state.
- **Preconditions**: Server running on port 8080.
- **Execution via Web UI**:
  - Click `Reset DB & Session` in the left sidebar or Operations Center.
  - Confirm the browser prompt.
- **Execution via cURL**:
  ```bash
  curl -s -u operator:operator123 -X POST http://localhost:8080/api/v1/reset
  ```
- **Expected Result**:
  - HTTP Status: `200 OK`
  - Response Body:
    ```json
    {
      "status": "RESET",
      "message": "All transactional tables cleared successfully"
    }
    ```
  - Database row counts in all 7 tables return to `0`.
- **Pass Criteria**: Status is `RESET`, 0 SQL exceptions.

---

### TC-02: Deterministic Synthetic Data Generation (Baseline Seed 42)
- **Objective**: Generate baseline synthetic data across 3 partner schemas and 3 business days with 10 anomaly types.
- **Preconditions**: Database reset.
- **Execution via Web UI**:
  - In **Operations Center**, set Seed = `42`, Loans = `1000`, Directory = `./data/seed_demo`.
  - Click `1. Generate Feeds`.
- **Execution via cURL**:
  ```bash
  curl -s -u operator:operator123 -X POST "http://localhost:8080/api/v1/generate?seed=42&loans=1000&outputDir=./data/seed_demo"
  ```
- **Expected Result**:
  - HTTP Status: `200 OK`
  - Response Body:
    ```json
    {
      "status": "GENERATED",
      "seed": 42,
      "loanCount": 1000,
      "outputDir": "./data/seed_demo",
      "message": "Feeds and ground truth generated successfully"
    }
    ```
  - Directory `./data/seed_demo` contains partner folders (`partner_alpha`, `partner_beta`, `partner_gamma`) and isolated `groundtruth/ground_truth.json`.
- **Pass Criteria**: All feed files generated, total records $\ge 5000$, isolated ground truth generated.

---

### TC-03: Seed Variation Test (Jury Scenario 1: Seed 9999)
- **Objective**: Prove determinism and reproducibility across arbitrary seeds.
- **Preconditions**: Baseline run completed.
- **Execution via Web UI**:
  - Go to **Jury Scenarios** panel $\to$ Click `Run Seed 9999`.
- **Execution via cURL**:
  ```bash
  curl -s -u operator:operator123 -X POST "http://localhost:8080/api/v1/generate?seed=9999&loans=1000&outputDir=./data/seed_9999"
  ```
- **Expected Result**:
  - Generates a completely new dataset in `./data/seed_9999` with different transaction references.
  - Re-running Seed 9999 a second time generates **byte-for-byte identical files** (verified by `GeneratorDeterminismTest`).
- **Pass Criteria**: Identical SHA-256 hashes for two runs of seed 9999; distinct from seed 42.

---

### TC-04: Multi-Format Feed Ingestion
- **Objective**: Ingest multi-format partner feeds (JSON, CSV, Pipe) into normalized database tables with SHA-256 hashes.
- **Preconditions**: Data generated in `./data/seed_demo`.
- **Execution via Web UI**:
  - Click `2. Ingest Multi-Format` in Operations Center.
- **Execution via cURL**:
  ```bash
  curl -s -u operator:operator123 -X POST "http://localhost:8080/api/v1/ingest?dataDir=./data/seed_demo"
  ```
- **Expected Result**:
  - HTTP Status: `200 OK`
  - Response Body:
    ```json
    {
      "totalRecords": 2986,
      "processedRecords": 2986,
      "quarantinedRecords": 0,
      "skippedDuplicates": 0,
      "batchStatus": "COMPLETED"
    }
    ```
  - Tables `source_batch`, `raw_source_record`, and `canonical_event` are populated.
- **Pass Criteria**: `processedRecords` equals `totalRecords`; `skippedDuplicates` is `0`.

---

### TC-05: Ingestion Idempotency & Deduplication Guard (Jury Scenario 2)
- **Objective**: Re-feed the exact same data directory and verify that 0 duplicate records are inserted and 0 financial mutations occur.
- **Preconditions**: TC-04 completed.
- **Execution via Web UI**:
  - Go to **Jury Scenarios** $\to$ Click `Re-Feed Ingestion` (or click `2. Ingest Multi-Format` again).
- **Execution via cURL**:
  ```bash
  curl -s -u operator:operator123 -X POST "http://localhost:8080/api/v1/ingest?dataDir=./data/seed_demo"
  ```
- **Expected Result**:
  - HTTP Status: `200 OK`
  - Response Body:
    ```json
    {
      "totalRecords": 2986,
      "processedRecords": 0,
      "quarantinedRecords": 0,
      "skippedDuplicates": 2986,
      "batchStatus": "COMPLETED"
    }
    ```
- **Pass Criteria**: `processedRecords == 0`, `skippedDuplicates == totalRecords`. Database row count unchanged.

---

### TC-06: Failure Isolation & Quarantine Handling
- **Objective**: Verify that records with missing required fields or negative values are quarantined without aborting valid records in the batch.
- **Preconditions**: Verified by automated test `FailureIsolationQuarantineTest`.
- **Automated Verification**:
  ```bash
  .\mvnw.cmd test -Dtest=FailureIsolationQuarantineTest
  ```
- **Expected Result**:
  - Invalid records stored in `raw_source_record` with `quarantined = TRUE` and non-null `quarantine_reason`.
  - Canonical events are generated only for valid rows.
  - Partial batch processing succeeds.
- **Pass Criteria**: 0 unhandled exceptions; valid records processed, invalid records isolated.

---

### TC-07: Batch Header Control-Total Drift Detection
- **Objective**: Flag a batch with `QUARANTINED` status if the declared amount or count does not match the sum of detail records.
- **Preconditions**: Batch with declared header discrepancy.
- **Expected Result**:
  - `source_batch.status` is updated to `QUARANTINED`.
  - Exception of type `CONTROL_TOTAL_MISMATCH` is emitted for Finance Controller.
- **Pass Criteria**: Discrepant batch blocked from processing.

---

### TC-08: 5-Tier Deterministic Reconciliation Execution
- **Objective**: Reconcile all ingested events across the 5 tiers (Exact, Composite, Timing, Probable, Unresolved).
- **Preconditions**: Feeds ingested (TC-04).
- **Execution via Web UI**:
  - Click `3. 5-Tier Reconcile` in Operations Center.
- **Execution via cURL**:
  ```bash
  curl -s -u operator:operator123 -X POST http://localhost:8080/api/v1/reconcile
  ```
- **Expected Result**:
  - HTTP Status: `200 OK`
  - Response Body:
    ```json
    {
      "totalLoans": 731,
      "exactMatches": 680,
      "compositeMatches": 6,
      "timingMatches": 0,
      "probableMatches": 41,
      "unresolvedBreaks": 4,
      "totalMatchedPaise": 340675973,
      "totalUnresolvedPaise": 22298935
    }
    ```
- **Pass Criteria**: Exact, Composite, Timing, Probable, and Unresolved counts match data distribution.

---

### TC-09: Hard Gate Evaluation Against Ground Truth
- **Objective**: Execute independent evaluation against isolated `ground_truth.json` and prove 0 false matches.
- **Preconditions**: TC-08 completed.
- **Execution via Web UI**:
  - Click `4. Evaluate Gate` (or navigate to **Gate Scorecard**).
- **Execution via cURL**:
  ```bash
  curl -s -u operator:operator123 "http://localhost:8080/api/v1/evaluate?groundTruthPath=./data/seed_demo/groundtruth/ground_truth.json"
  ```
- **Expected Result**:
  - `phase1Gate.gateStatus`: `"PASSED"`
  - `phase1Gate.falseMatchCount`: `0`
  - `phase1Gate.falseMatchExposureValue`: `"₹0.00"`
  - `phase1Gate.controlTotalDelta`: `"₹0.00"`
  - `phase1Gate.straightThroughRate`: $\ge 90\%$
- **Pass Criteria**: Gate status is `PASSED` with 0 false matches.

---

### TC-10: Anomaly Coverage & Confusion Matrix Verification
- **Objective**: Verify that all 10 mandatory anomaly classes are present and correctly routed.
- **Execution via Web UI**:
  - Inspect the **Confusion Matrix** on the **Gate Scorecard** tab.
- **Expected Result**:
  - Rows for `AMOUNT_MISMATCH`, `STATUS_MISMATCH`, `MISSING_BANK_LEG`, `MISSING_LMS_LEG`, `TIMING_DIFFERENCE`, `COMPOSITE_SPLIT`, `ORPHAN_REVERSAL`, `DUPLICATE_BANK_EVENT`, `SCHEMA_BREACH`, `BATCH_TOTAL_MISMATCH`.
  - Zero anomalies classified as `EXACT` (except declared batch totals where detail rows match).
- **Pass Criteria**: All 10 anomaly types represented in matrix.

---

### TC-11: Exception Queue & Dynamic SLA/Priority Scoring
- **Objective**: Verify breaks are prioritized by financial exposure and SLA urgency.
- **Preconditions**: Reconciliation executed.
- **Execution via Web UI**:
  - Open **Exception Queue** panel.
- **Execution via cURL**:
  ```bash
  curl -s -u operator:operator123 http://localhost:8080/api/v1/exceptions
  ```
- **Expected Result**:
  - High exposure items ($> \text{₹}10,000$) assigned `CRITICAL` priority.
  - Ownership assigned appropriately:
    - `AMOUNT_MISMATCH` $\to$ `FINANCE_OPERATIONS`
    - `STATUS_MISMATCH` $\to$ `LENDING_OPERATIONS`
    - `MISSING_FEED_LEG` $\to$ `PARTNER_INTEGRATION_ENG`
- **Pass Criteria**: Valid SLA countdown timestamps, correct owner mapping.

---

### TC-12: Maker Exception Override with Audit Trail
- **Objective**: Record an authorized override on an exception with mandatory business justification.
- **Preconditions**: At least one `OPEN` exception.
- **Execution via Web UI**:
  - On an open exception row, click `Override`.
  - Enter justification: `"Approved by Partner Credit Committee"`.
  - Click `Confirm Override`.
- **Execution via cURL**:
  ```bash
  curl -s -u operator:operator123 -X POST "http://localhost:8080/api/v1/exceptions/{exceptionId}/override?reason=Approved+by+Partner+Credit+Committee"
  ```
- **Expected Result**:
  - HTTP Status: `200 OK`
  - `status` transitions to `OVERRIDDEN`.
  - Audit log entry generated in `audit_log` table with `action = 'OVERRIDE'` and `actor_id = 'operator'`.
- **Pass Criteria**: Record shows `Audited`, audit trail contains reason and actor.

---

### TC-13: Phase 2 Root-Cause Clusters Intelligence
- **Objective**: Verify systemic grouping of exceptions into partner failure patterns.
- **Execution via Web UI**:
  - Open **Root-Cause Clusters** panel.
- **Execution via cURL**:
  ```bash
  curl -s -u operator:operator123 http://localhost:8080/api/v1/exceptions/clusters
  ```
- **Expected Result**:
  - Returns synthesized cluster cards with:
    - `clusterKey` (e.g. `PARTNER_GAMMA::AMOUNT_MISMATCH`)
    - `exceptionCount` & `totalExposureInr`
    - `rootCauseHypothesis`
    - `recommendedRemediation`
- **Pass Criteria**: Clusters synthesized with actionable remediation recommendations.

---

### TC-14: Batch Close Equation 1 (Balance Sheet Equation)
- **Objective**: Evaluate balance equation: $\text{Opening} + \text{Valid Movements} - \text{Reversals} = \text{Closing}$.
- **Execution via Web UI**:
  - Open **Batch Close Workbench** $\to$ Select batch $\to$ Click `Evaluate Close Equation`.
- **Execution via cURL**:
  ```bash
  curl -s -u operator:operator123 -X POST "http://localhost:8080/api/v1/close/evaluate?batchId=B-PARTNER_ALPHA-ORIGINATOR-20240115"
  ```
- **Expected Result**:
  - Equation evaluates with mathematically balanced opening and closing balances.
- **Pass Criteria**: $\text{Opening} + \text{Valid} - \text{Reversals} == \text{Closing}$.

---

### TC-15: Zero-Tolerance 1-Paise Break Gate (Jury Scenario 3)
- **Objective**: Prove that even a 1-paise variance forces the close decision to `HOLD`.
- **Preconditions**: Verified by automated test `CloseEquationBreakTest`.
- **Automated Verification**:
  ```bash
  .\mvnw.cmd test -Dtest=CloseEquationBreakTest
  ```
- **Expected Result**:
  - Balanced batch returns `decision = 'CLOSE'`.
  - Injecting 1 paise of unaccounted delta immediately flips decision to `HOLD`.
  - `blocking_exceptions` JSON contains the break details.
- **Pass Criteria**: Decision is `HOLD`, `unaccountedPaise != 0`.

---

### TC-16: Maker-Checker Segregation of Duties Enforcement (Jury Scenario 4)
- **Objective**: Prove that an Operator cannot approve batch close, and an Approver who overrode an exception cannot approve close.
- **Preconditions**: User logged in as Operator.
- **Execution via Web UI**:
  - In **Batch Close Workbench**, click `Approve Close (Checker)` while signed in as Operator.
- **Execution via cURL**:
  ```bash
  curl -s -u operator:operator123 -X POST "http://localhost:8080/api/v1/close/approve?closeId={closeId}"
  ```
- **Expected Result**:
  - HTTP Status: `403 Forbidden`
  - Error Payload:
    ```json
    {
      "error": "SEGREGATION_VIOLATION",
      "message": "Segregation of duties violation: Maker cannot be Checker"
    }
    ```
- **Pass Criteria**: HTTP `403 Forbidden` returned; unauthorized sign-off blocked.

---

### TC-17: Checker Close Certification
- **Objective**: Verify that an authorized Approver (who did not override exceptions in that batch) can certify close.
- **Execution via Web UI**:
  - Switch account to **Approver (Checker)** via top-right profile switcher.
  - Click `Approve Close (Checker)`.
- **Execution via cURL**:
  ```bash
  curl -s -u approver:approver123 -X POST "http://localhost:8080/api/v1/close/approve?closeId={closeId}"
  ```
- **Expected Result**:
  - HTTP Status: `200 OK`
  - Response contains certified `CloseSummaryEntity` with `decided_by = 'approver'`.
- **Pass Criteria**: Close certified successfully.

---

### TC-18: Cryptographic Lineage Trace (Page 12 Jury Question 5)
- **Objective**: Inspect the immutable evidence chain from raw payload to final match decision.
- **Execution via Web UI**:
  - Open **Audit Lineage Trace** $\to$ Enter `LOAN-000001` $\to$ Click `Verify Lineage`.
- **Execution via cURL**:
  ```bash
  curl -s -u operator:operator123 http://localhost:8080/api/v1/audit/trace/LOAN-000001
  ```
- **Expected Result**:
  - Displays raw string payload.
  - Displays 64-character SHA-256 digest (`payloadHash`).
  - Displays normalized canonical event and final match decision.
- **Pass Criteria**: Complete cryptographic chain from raw record to match decision.

---

### TC-19: Concurrency Stress Test
- **Objective**: Verify that concurrent reconciliation requests do not produce race conditions or unique constraint collisions.
- **Execution via PowerShell**:
  ```powershell
  $job1 = Start-Job -ScriptBlock { curl.exe -s -u operator:operator123 -X POST http://localhost:8080/api/v1/reconcile };
  $job2 = Start-Job -ScriptBlock { curl.exe -s -u operator:operator123 -X POST http://localhost:8080/api/v1/reconcile };
  Wait-Job $job1, $job2; Receive-Job $job1; Receive-Job $job2;
  ```
- **Expected Result**:
  - Both requests return `200 OK` with identical reconciliation totals.
  - Zero `500 Internal Server Error` responses.
  - Zero database constraint violations.
- **Pass Criteria**: Both parallel jobs succeed cleanly.

---

### TC-20: Full Automated Hard-Gate Test Suite
- **Objective**: Run the entire JUnit 5 and ArchUnit regression suite covering all gate requirements.
- **Execution**:
  ```bash
  .\mvnw.cmd test
  ```
- **Expected Result**:
  ```
  [INFO] Tests run: 13, Failures: 0, Errors: 0, Skipped: 0
  [INFO] BUILD SUCCESS
  ```
- **Pass Criteria**: 13/13 tests pass with 0 failures and 0 errors.

---

## 4. Test Execution Cheatsheet

### One-Click Automated Pipeline Run
To run the entire pipeline end-to-end:
```bash
# 1. Reset
curl -s -u operator:operator123 -X POST http://localhost:8080/api/v1/reset

# 2. Generate (1000 loans, Seed 42)
curl -s -u operator:operator123 -X POST "http://localhost:8080/api/v1/generate?seed=42&loans=1000&outputDir=./data/seed_demo"

# 3. Ingest
curl -s -u operator:operator123 -X POST "http://localhost:8080/api/v1/ingest?dataDir=./data/seed_demo"

# 4. Reconcile
curl -s -u operator:operator123 -X POST http://localhost:8080/api/v1/reconcile

# 5. Evaluate Gate
curl -s -u operator:operator123 "http://localhost:8080/api/v1/evaluate?groundTruthPath=./data/seed_demo/groundtruth/ground_truth.json"
```

Or simply click **⚡ Execute End-to-End Pipeline** on the Web UI at **`http://localhost:8080`**.


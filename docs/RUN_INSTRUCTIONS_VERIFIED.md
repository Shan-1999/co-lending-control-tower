# Vivriti Co-Lending Control Tower: Clean Environment Runbook & Verification Guide
**Document ID:** VIVRITI-RUN-2026 | **Author:** Control Tower Engineering | **Classification:** Operations Runbook

---

## 1. Prerequisites & Environment Setup

The application has been verified from a completely clean environment on both **Windows (PowerShell / Command Prompt)** and **Linux / macOS (Bash)**.

### 1.1 Minimum System Requirements
- **Java Development Kit (JDK):** Version **21** (Temurin, Oracle, Corretto, or Microsoft Build of OpenJDK).
- **Build Tool:** Bundled Maven Wrapper (`./mvnw` on Linux/macOS or `.\mvnw.cmd` on Windows) — no manual Maven installation required.
- **Database:**
  - **Production Profile (`default`):** PostgreSQL 16 (listening on `localhost:5432` with user `postgres`, database `controltower`).
  - **Testing Profile (`test`):** In-Memory H2 database (automatically spun up with zero external dependencies).
- **Web Browser:** Google Chrome, Microsoft Edge, or Mozilla Firefox for the interactive operations dashboard.

---

## 2. One-Click Execution Scripts

### 2.1 Windows Clean Run
Open a Command Prompt or PowerShell terminal in the repository root directory:
```cmd
run.bat
```
*The script automatically detects Java 21, checks PostgreSQL availability (falling back to H2 if offline), applies schema migrations, generates 1,000 synthetic loans, executes ingestion and 5-tier reconciliation, runs gate evaluation, and executes the 13 automated tests.*

### 2.2 Linux / macOS Clean Run
Open a terminal in the repository root directory:
```bash
chmod +x run.sh scripts/*.sh
./run.sh
```

---

## 3. Manual Step-by-Step Execution

### Step 1: Run the Automated Hard-Gate Test Suite
Execute the entire test suite covering failure isolation, determinism, idempotency, close equation breaks, and maker-checker segregation:
```bash
# Windows
.\mvnw.cmd test

# Linux / macOS
./mvnw test
```
**Expected Output:**
```
[INFO] Results:
[INFO] Tests run: 13, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

### Step 2: Start the Control Tower Application
Launch the Spring Boot application on port 8080:
```bash
# Windows
.\mvnw.cmd spring-boot:run

# Linux / macOS
./mvnw spring-boot:run
```
Wait until the console logs show:
```
Started ControlTowerApplication in X.XXX seconds
Tomcat started on port 8080 (http) with context path '/'
```

### Step 3: Access the Operations Dashboard
Open your browser and navigate to:
```
http://localhost:8080
```
*(No login required for dashboard landing; user profile switcher controls role-based actions).*

---

## 4. Interactive Web UI Walkthrough & Verification

### 4.1 Workstream 1: Synthetic Data Generation & Data Quality Report
1. On the left sidebar or in the Operations Center, click **1. Generate Feeds**.
2. To inspect the generated data quality report:
   - Click the new **"📊 Data Quality Report"** item on the left sidebar.
   - Or click **"📊 View Quality Report"** in the Operations Center.
3. Review the live metrics:
   - **Total Loans:** 2,000 unique loans.
   - **Feed Records:** 5,972 multi-format records across Partner Alpha (JSON), Beta (CSV), Gamma (Pipe).
   - **Overall Anomaly Rate:** $\ge 5.00\%$ (e.g. 9.40%).
   - **All 10 Anomaly Classes:** Missing Bank Leg, Missing LMS Leg, Duplicate Callback, Amount Mismatch, Status Mismatch, Timing Difference, Composite Split, Orphan Reversal, Schema Breach, Batch Total Drift.
   - **Raw Markdown:** View line-by-line or click **"📋 Copy Markdown"**.

### 4.2 Workstream 2: Multi-Format Feed Ingestion
1. In the Operations Center, click **2. Ingest Multi-Format**.
2. Inspect the terminal stream:
   - SHA-256 payload hashes computed for all records.
   - Idempotency guard validates zero duplicates.
   - Schema breaches isolated to `raw_source_record` with `quarantined=true`.
   - Valid records normalized into `canonical_event`.

### 4.3 Workstream 3: 5-Tier Reconciliation Engine
1. Click **3. 5-Tier Reconcile**.
2. Engine processes events through the chain of responsibility:
   - Exact 3-leg matches reconciled straight-through.
   - Composite splits balanced across multiple settlement debits.
   - Post-cutoff callbacks placed in 24h grace window (`PENDING_GRACE_PERIOD`).
   - Unresolved breaks routed to the Exception Queue.

### 4.4 Workstream 4: Gate Scorecard & Evaluation
1. Click **4. Evaluate Gate** or navigate to **"🛡️ Gate Scorecard"**.
2. Verify the hard gate:
   - **False Matches:** **0 Loans** (Tolerance: 0).
   - **False Match Financial Exposure:** **₹0.00** (Tolerance: ₹0.00).
   - **Gate Status:** **PASSED** (in green).
   - Inspect the Confusion Matrix across all 10 anomaly types.

### 4.5 Workstream 5: Batch Close & Maker-Checker Verification
1. Navigate to **"⚖️ Batch Close Workbench"**.
2. Select batch `B-PARTNER_ALPHA-BANK-20240115` from the dropdown:
   - Click **Evaluate Close**.
   - Notice the decision renders: **`DECISION: HOLD`** (in bold red).
   - Reason: The batch contains quarantined records and partner exceptions. The equation identifies all blocking exceptions and halts close.
3. Switch the dropdown to a clean validated batch:
   - Click **Evaluate Close**.
   - Decision renders: **`DECISION: CLOSE`** (in green) with ₹0.00 unaccounted delta.
4. Test Four-Eyes Segregation of Duties:
   - Switch active profile to **Operator (Maker)** using the top-right profile avatar.
   - Attempt to click **Approve Close** $\rightarrow$ blocked with HTTP 403.
   - Switch to **Approver (Checker)** $\rightarrow$ approval succeeds only if this user did not execute an override on the same batch.

### 4.6 Workstream 6: Audit Lineage Trace
1. Navigate to **"📜 Audit Lineage Trace"**.
2. Enter loan reference `LOAN-000001` and click **Trace Lineage**.
3. View the complete cryptographic chain:
   $$\text{Raw Source Payload} \longrightarrow \text{SHA-256 Hash} \longrightarrow \text{Canonical Event} \longrightarrow \text{Reconciliation Decision}$$

---

## 5. Production Observability Endpoints

All actuator and documentation endpoints are exposed and operational:

| Endpoint | URL | Description |
| :--- | :--- | :--- |
| **System Logfile** | `http://localhost:8080/actuator/logfile` | Live SLF4J application log stream including evaluation scorecards. |
| **System Health** | `http://localhost:8080/actuator/health` | PostgreSQL connectivity, disk space, and application liveness. |
| **Swagger / OpenAPI** | `http://localhost:8080/swagger-ui/index.html` | Interactive REST API testing console with OpenAPI 3.0 schema. |
| **Dynamic Loggers** | `http://localhost:8080/actuator/loggers` | Real-time adjustment of log levels (TRACE, DEBUG, INFO) without restart. |
| **JVM Metrics** | `http://localhost:8080/actuator/metrics` | Detailed metrics for memory, threads, garbage collection, and HikariCP. |
| **HTTP Trace** | `http://localhost:8080/actuator/httpexchanges`| History of recent HTTP request/response exchanges. |

---

## 6. Verification Checklist Summary

- [x] Java 21 compile clean with zero compilation warnings or errors.
- [x] All 13 unit, architecture, and integration tests pass cleanly with `BUILD SUCCESS`.
- [x] ArchUnit test enforces strict isolation of Ground Truth from reconciliation engine.
- [x] Batch Close evaluates to `HOLD` on quarantined batches and `CLOSE` on balanced batches.
- [x] Quality Report generated and viewable in UI on every run.
- [x] Four-Eyes governance strictly prevents Maker from acting as Checker.
- [x] Zero false matches, zero false exposure, and zero unaccounted paise verified.

---
*Vivriti Co-Lending Control Tower Operations Runbook — Verified Clean Environment.*


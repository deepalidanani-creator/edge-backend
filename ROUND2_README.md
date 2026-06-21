# Round 2 — Employee / Cohort CSV Upload

Design and implementation notes for the L&D Admin CSV upload slice (Emeritus Buckets, Round 2 brief).  
Round 1 scheduling docs: **[README.md](README.md)** and **[SCHEMA.md](SCHEMA.md)**.

---

## Scope

| Phase | Status | Description |
|-------|--------|-------------|
| **Phase 1 — Preview** | **Implemented** | Parse CSV, reconcile against tenant DB **read-only**, return per-row operations for admin review. **No DB writes.** |
| **Phase 2 — Commit** | Planned | Admin confirm, DB apply, async processing, idempotency — see **Technical Design Document.docx** (Phase 2 planning). |

---

## End-to-end flow (implemented today)

```
┌─────────────────────────────────────────────────────────────┐
│  POST /v1/upload/emp-with-cohort-assignment                 │
│  Headers: X-Tenant-Id | X-User-Role | Idempotency-Key*      │
└───────────────────────────┬─────────────────────────────────┘
                            │
         ┌──────────────────▼──────────────────┐
         │  FILE LEVEL (controller) — fail all  │
         │  role | empty | .csv | 10MB | parse  │
         └──────────────────┬──────────────────┘
                            │ OK
         ┌──────────────────▼──────────────────┐
         │  reconcileRows (NO DB WRITES)        │
         │  ┌────────────┐   ┌───────────────┐  │
         │  │ PASS 1     │   │ PASS 2        │  │
         │  │ validate   │──▶│ CREATE/UPDATE │  │
         │  │ duplicates │   │ cohort diff   │  │
         │  └────────────┘   └───────────────┘  │
         │         ▲ reads: tenant_cohort,       │
         │                employee (CSV ids)    │
         └──────────────────┬──────────────────┘
                            │
         ┌──────────────────▼──────────────────┐
         │  JSON: valid | totalRows | rows[]     │
         │  each row: category | ops | errors    │
         └───────────────────────────────────────┘

* Idempotency-Key: required header; replay not implemented in Phase 1
```

---

## Admin UI mapping (`rowCategory`)

| `rowCategory` | Admin sees | Typical `operations` (preview) |
|---------------|------------|--------------------------------|
| **CORRECTION** | Fix row, re-upload | `NO_OP` + `errors[]` |
| **CREATE** | Confirm new hires | `CREATE_EMPLOYEE` (+ `ADD_COHORT_MEMBERSHIP` if cohorts in CSV) |
| **UPDATE** | Confirm changes to existing employees | `UPDATE_EMPLOYEE` and/or `ADD_COHORT_MEMBERSHIP` / `REMOVE_COHORT_MEMBERSHIP` |
| **SKIP_OP** | FYI — no change needed | `NO_OP` |

Response wrapper: **`CSVEmployeeCohortResponse`** — `valid`, `totalRows`, `rows[]`.  
Each row: **`CsvEmployeeCohortRowResult`** — `rowNumber`, `valid`, `rowCategory`, `operations[]`, `errors[]`, `data` (parsed CSV fields).

---

## HTTP API

**Endpoint:** `POST /v1/upload/emp-with-cohort-assignment`  
**Content:** `multipart/form-data`, part name `file` (CSV).

| Header | Purpose |
|--------|---------|
| `X-Tenant-Id` | Tenant scope (also enforced by `TenantAccessInterceptor` on this path) |
| `X-User-Role` | Must be `LND_ADMIN` for upload |
| `Idempotency-Key` | Required on request; Phase 1 does not store or replay |

| Outcome | HTTP | Body |
|---------|------|------|
| File / role / parse fatal | `403` / `400` | `{ "error": "..." }` |
| Reconcile OK, all rows valid | `200` | Full `CSVEmployeeCohortResponse`, `valid: true` |
| Reconcile done, some rows invalid | `400` | Full response, `valid: false` — **partial row results still returned** |

**CSV columns:** `employee_id`, `email`, `name`, `role` (optional), `cohort_names` (semicolon-separated names), `start_date` (optional, ISO date — parsed in DTO, not used in reconcile yet).

---

## What is implemented

### Layer summary

| Layer | Components | Notes |
|-------|------------|-------|
| **Controller** | `FileUploadController` | File guards + parse + reconcile |
| **Service** | `FileUploadService` | `parseCSV()`, **`reconcileRows()`** (core) |
| **DTOs** | `CSVEmployeeCohortDto`, response types, `RowFieldError`, enums | Match brief |
| **Entities** | `Employee` (extended), `TenantCohort` | See **[SCHEMA_ROUND2.md](SCHEMA_ROUND2.md)** |
| **Repositories** | `EmployeeRepository`, `TenantCohortRepository` | Read-only in reconcile |
| **Tests** | Reconciliation (18), integration (7), controller (1) | See Tests section below |

### Dependencies

- **OpenCSV** in `pom.xml` for CSV parsing.

---

## Database changes (Round 2)

Round 2 adds **`tenant_cohort`**, extends **`employee`** with `name`, `email_id`, and `role`, and uses existing **`employee_cohorts`** for membership diff.

Full table definitions, ERD, seed data, repository notes, and CSV column mapping:

**→ [SCHEMA_ROUND2.md](SCHEMA_ROUND2.md)**

---

## Repositories (reconcile usage)

| Repository | Method | Role in reconcile |
|------------|--------|-------------------|
| `TenantCohortRepository` | `findByTenantId(tenantId)` | Build in-memory catalog: name → cohort, id → cohort |
| `EmployeeRepository` | `findAllById(csvEmployeeIds)` | Load **only employees referenced in CSV**; filter by `tenantId` |

**Design choice:** targeted employee load (not full `findByTenantId`) — scales when tenant has many employees but CSV has ~1,500 rows.

---

## Validation and reconciliation logic

### Pass 1 — row validation (in-file)

Runs on every row. Failures set `valid = false`, `rowCategory = CORRECTION`, `operations = [NO_OP]`.

| Code | Condition |
|------|-----------|
| `REQUIRED_FIELD_MISSING` | Missing `employee_id`, `email`, or `name` |
| `DUPLICATE_EMPLOYEE` | Same `employee_id` on multiple rows (all involved rows marked) |
| `DUPLICATE_EMAIL` | Same email on multiple rows (all involved rows marked) |

Pass 2 **skips** rows where `valid = false`.

### Pass 2 — reconcile (read DB, emit operations)

**Snapshot loaded once:** tenant cohort catalog + employees for distinct CSV ids.

**New employee (`employee_id` not in tenant DB):**

- No cohorts → `CREATE` + `CREATE_EMPLOYEE`
- Cohorts listed → validate names against catalog → `CREATE` + `CREATE_EMPLOYEE` + `ADD_COHORT_MEMBERSHIP`, or `UNKNOWN_COHORT`

**Existing employee:**

- Resolve DB cohort ids → names; orphan id → `ORPHAN_COHORT_MEMBERSHIP`
- Unknown CSV cohort name → `UNKNOWN_COHORT`
- Cohort diff (CSV vs DB): may emit **both** `ADD_COHORT_MEMBERSHIP` and `REMOVE_COHORT_MEMBERSHIP` on same row
- Empty CSV `cohort_names` but DB has cohorts → `REMOVE_COHORT_MEMBERSHIP` (**replace mode**)
- Email change vs DB → `EMAIL_CONFLICT` (not `UPDATE_EMPLOYEE`)
- Name/role change, same email → `UPDATE_EMPLOYEE`
- No cohort ops and details unchanged → `SKIP_OP` + `NO_OP`

**Top-level `valid`:** `true` only if **every** row has `valid = true`.

### `ReconciliationOperation` enum

`CREATE_EMPLOYEE`, `UPDATE_EMPLOYEE`, `ADD_COHORT_MEMBERSHIP`, `REMOVE_COHORT_MEMBERSHIP`, `NO_OP`

### Row field names in errors (API)

| Error context | `RowFieldError.field` |
|---------------|------------------------|
| Missing / duplicate email, email conflict | `emailId` |
| Missing employee id / duplicate id | `employeeId` |
| Missing name | `name` |
| Cohort issues | `cohortName` |

---

## File-level validation (controller)

Entire request rejected — no row list.

| Check | Error shape |
|-------|-------------|
| Not `LND_ADMIN` | `403` — upload-specific message |
| Null / empty file | `400` |
| Not `.csv` extension | `400` — **extension check**, not MIME/Tika (Tika noted as trade-off) |
| Size &gt; 10 MB | `400` |
| OpenCSV parse failure | `400` — `{ "error": "Failed to parse CSV: ..." }` |

---

## Tests

```bat
mvnw.cmd test -Dtest=FileUploadServiceReconciliationTests,FileUploadIntegrationTests,FileUploadControllerTests
```

| Test class | Count | Covers |
|------------|-------|--------|
| `FileUploadServiceReconciliationTests` | 18 | Reconciler: create/update, cohort diff, duplicates, email conflict, orphan cohort, `valid` flag, etc. |
| `FileUploadIntegrationTests` | 7 | Multipart HTTP + H2 seed: happy/update/error CSVs, 403/400 file guards, missing tenant |
| `FileUploadControllerTests` | 1 | Non-admin forbidden message (unit) |

Full suite (Round 1 + Round 2): `mvnw.cmd test` or `test.bat`.

---

## Sample CSV files

Checked-in under **[samples/](samples/)** for manual and integration tests:

| File | Purpose |
|------|---------|
| `upload-happy.csv` | CREATE + SKIP_OP — all rows valid (`200`) |
| `upload-update.csv` | Name change + cohort ADD/REMOVE on `emp-001` |
| `upload-errors.csv` | Duplicate id, unknown cohort, email conflict (`400`, partial rows) |
| `empty.csv` / `not-csv.txt` | File-level error curl demos |

Use **semicolon** separators in `cohort_names` (e.g. `Senior Leadership;Managers Q2`).

---

## Manual API tests (curl)

Full step-by-step commands for every happy and error scenario → **[CURL_ROUND2.md](CURL_ROUND2.md)**.

Quick smoke (from repo root, app running via `run.bat`):

```bat
curl -i -X POST http://localhost:8080/v1/upload/emp-with-cohort-assignment ^
  -H "X-Tenant-Id: vantage-fi" ^
  -H "X-User-Role: LND_ADMIN" ^
  -H "Idempotency-Key: preview-demo-001" ^
  -F "file=@samples/upload-happy.csv"
```

---

## Project layout (Round 2)

```
src/main/java/com/emeritus/edge_backend/
  controller/FileUploadController.java
  service/FileUploadService.java          ← parseCSV + reconcileRows
  dto/request/CSVEmployeeCohortDto.java
  dto/response/CSVEmployeeCohortResponse.java
  dto/response/CsvEmployeeCohortRowResult.java
  dto/response/RowFieldError.java
  dto/response/RowCategory.java
  dto/response/ReconciliationOperation.java
  entity/TenantCohort.java
  entity/Employee.java
  repository/TenantCohortRepository.java
  repository/EmployeeRepository.java
  mapper/CSVEmployeeCohortEntityMapper.java
src/test/java/.../FileUploadServiceReconciliationTests.java
src/test/java/.../FileUploadIntegrationTests.java
samples/                                  ← upload-happy.csv, upload-errors.csv, etc.
CURL_ROUND2.md                            ← manual curl test matrix
src/main/resources/data.sql               ← tenant_cohort + employee seed
```

---

## Trade-offs (Round 2)

1. **Extension vs content-type** — `.csv` suffix only; Apache Tika possible later.
2. **10 MB / messages in Java** — move to `application.properties` per environment.
3. **No email format validator** — non-blank string assumed valid.
4. **Exact cohort name match** — no fuzzy/case-insensitive match (design doc Q6).
5. **Replace-mode cohorts** — empty CSV cohort list removes all DB memberships on existing employees.
6. **Email uniqueness** — DB constraint on `email_id` globally; cross-tenant email collision not reconciled separately.
7. **Preview-only** — reconcile never writes; safe for admin review before irreversible commit.

---

## Related documents

| Document | Content |
|----------|---------|
| [SCHEMA_ROUND2.md](SCHEMA_ROUND2.md) | Round 2 DB schema — `tenant_cohort`, extended `employee`, seed data |
| [README.md](README.md) | Round 1 — masterclass scheduling |
| [CURL_ROUND2.md](CURL_ROUND2.md) | Round 2 manual curl commands |
| [SCHEMA.md](SCHEMA.md) | Round 1 schema reference |
| Technical Design Document.docx | Detailed narrative, 8 design questions, **Phase 2 planning** |

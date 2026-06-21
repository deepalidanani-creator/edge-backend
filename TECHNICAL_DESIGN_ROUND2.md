# Round 2 — Employee CSV Upload  
## Technical Design (Implemented)

**What it does:** L&D admin uploads a CSV. The API validates the file and each row, compares against tenant data, and returns a **preview** of proposed changes. **Nothing is written to the database.**

---

### At a glance

| | |
|---|---|
| **Endpoint** | `POST /v1/upload/emp-with-cohort-assignment` |
| **Headers** | `X-Tenant-Id` · `X-User-Role: LND_ADMIN` · `Idempotency-Key`¹ |
| **File** | `.csv`, max 10 MB, multipart field `file` |
| **Success** | `200` — all rows valid |
| **Partial errors** | `400` — invalid rows flagged; **all rows still returned** |
| **File rejected** | `403` / `400` — single `{ "error": "..." }`, no row list |

¹ Header required; not stored or replayed in current code.

**CSV columns:** `employee_id`, `email`, `name`, `role` (optional), `cohort_names` (optional, semicolon-separated names), `start_date` (optional, not used in reconcile).

---

### Flow

```
  Upload CSV
      │
      ▼
  ┌─────────────────────────┐
  │ File checks (controller)│── fail ──▶ 403 / 400
  │ role · empty · .csv · size │
  └───────────┬─────────────┘
              │ OK
              ▼
  ┌─────────────────────────┐
  │ reconcileRows (service) │
  │  Pass 1 — file rules    │  duplicates, required fields
  │  Pass 2 — vs database   │  CREATE / UPDATE / cohort diff
  └───────────┬─────────────┘     (reads tenant_cohort + employee)
              ▼
  JSON: valid · totalRows · rows[]
  (each row: category · operations · errors · parsed data)
```

---

### Components

| Layer | Class | Role |
|-------|-------|------|
| Security | `TenantAccessInterceptor` | Requires tenant header |
| Controller | `FileUploadController` | File validation, orchestration |
| Service | `FileUploadService` | `parseCSV()` + **`reconcileRows()`** |
| Data | `TenantCohortRepository`, `EmployeeRepository` | Read-only lookups |
| Types | DTOs + `RowCategory` + `ReconciliationOperation` | Response shape |

**Core logic:** `FileUploadService.reconcileRows()` — 18 unit tests + 7 integration tests.

---

### Database (what reconcile reads)

| Table | Change | Used for |
|-------|--------|----------|
| `tenant_cohort` | **New** — cohort display name → id | Match CSV `cohort_names` |
| `employee` | **Added** `name`, `email_id`, `role` | Compare / create preview |
| `employee_cohorts` | Unchanged | Membership diff |

**Reads per upload:** all cohorts for tenant + employees whose ids appear in the CSV (not full tenant scan).

---

### When the whole file is rejected

| Reason | HTTP |
|--------|------|
| Not LND_ADMIN | 403 |
| Empty file, wrong extension, > 10 MB, parse error | 400 |

---

### Row errors (other rows still processed)

| Code | Meaning |
|------|---------|
| `REQUIRED_FIELD_MISSING` | Missing employee id, email, or name |
| `DUPLICATE_EMPLOYEE` | Same id twice in file |
| `DUPLICATE_EMAIL` | Same email twice in file |
| `UNKNOWN_COHORT` | Cohort name not in tenant catalog |
| `ORPHAN_COHORT_MEMBERSHIP` | Employee linked to cohort id missing from catalog |
| `EMAIL_CONFLICT` | Existing employee — email cannot change via CSV |

---

### Reconcile outcomes (valid rows)

| Situation | Admin sees | Operations |
|-----------|------------|------------|
| New employee | CREATE | `CREATE_EMPLOYEE` (+ cohort add if listed) |
| No changes | SKIP_OP | `NO_OP` |
| Name or role change | UPDATE | `UPDATE_EMPLOYEE` |
| Cohort change | UPDATE | `ADD_COHORT_MEMBERSHIP` / `REMOVE_COHORT_MEMBERSHIP` |
| Bad row | CORRECTION | `NO_OP` + errors |

**Policies:** Cohort list in CSV is the **desired set** (replace mode). Email change is **blocked**. Top-level `valid` is true only when **every** row is valid.

---

### Design questions (brief)

| | Answer (implemented today) |
|---|---------------------------|
| **Outcome** | Preview JSON only — no DB writes |
| **Processing** | Synchronous; parse → 2 DB reads → in-memory reconcile |
| **Partial failure** | Row-level; one bad row does not hide others |
| **Repeat upload** | Idempotency header required; not replayed yet |
| **Scale (~1.5k rows)** | Load employees by CSV ids only; reconcile in memory |
| **Cohort names** | Exact match to tenant catalog display name |
| **Cohort list** | Replace mode — CSV is source of truth |
| **Email** | Cannot change on existing employee; globally unique in DB |

---

### Out of scope

- **Writes:** `EmployeeWriteRepository` is implemented (create, update, cohort add/remove). Confirm/apply from preview is **not wired** — orchestrator + staging are the remaining step.
- **Still open:** idempotency replay, mapper logic, `start_date` in reconcile.

---

### Trade-offs

Extension-only `.csv` check · 10 MB limit in code · no email format validation · read-only reconcile

**Manual tests:** `samples/` + `CURL_ROUND2.md` · **Schema detail:** `SCHEMA_ROUND2.md`

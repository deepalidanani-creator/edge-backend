# curl command reference — Round 2 CSV upload

Start the API first: `run.bat` (listens on `http://localhost:8080`).

**Tenant used below:** `vantage-fi` (seed employees `emp-001`, `emp-002`; cohort catalog in `data.sql`).

**Sample CSV files:** [`samples/`](samples/) — use paths below from the repo root.

**Cohort names:** semicolon-separated in `cohort_names` (e.g. `Senior Leadership;Managers Q2`).

**Windows:** commands use `^` line breaks. **Mac/Linux:** replace `^` with `\`.

**Tip:** add `-i` to print the HTTP status line (`200 OK`, `400 Bad Request`, etc.).

Design notes and reconciler rules → **[ROUND2_README.md](ROUND2_README.md)**.

---

## Scenario index

| # | Scenario | File | Expect |
|---|----------|------|--------|
| H1 | All rows valid (CREATE + SKIP + CREATE) | `upload-happy.csv` | `200`, `"valid":true` |
| H2 | Name + cohort diff UPDATE | `upload-update.csv` | `200`, `UPDATE`, ADD + REMOVE ops |
| E1 | Wrong role | any CSV | `403` |
| E2 | Empty file | (none) | `400` |
| E3 | Not `.csv` extension | `.txt` upload | `400` |
| E4 | Row-level errors (duplicates, unknown cohort, email conflict) | `upload-errors.csv` | `400`, `"valid":false`, partial `rows[]` |
| E5 | Missing `X-Tenant-Id` | `upload-happy.csv` | `400` |

---

## 1. Happy path — preview all valid rows

Creates `emp-003` and `emp-004`, no-op on `emp-001` (matches seed).

```bat
curl -i -X POST http://localhost:8080/v1/upload/emp-with-cohort-assignment ^
  -H "X-Tenant-Id: vantage-fi" ^
  -H "X-User-Role: LND_ADMIN" ^
  -H "Idempotency-Key: curl-r2-happy-001" ^
  -F "file=@samples/upload-happy.csv"
```

**Expect:** `200 OK` — `"valid":true`, `"totalRows":3`.

| Row | `employee_id` | Typical `rowCategory` | Notes |
|-----|---------------|----------------------|-------|
| 2 | `emp-003` | `CREATE` | New employee, no cohorts |
| 3 | `emp-001` | `SKIP_OP` | Matches seed exactly |
| 4 | `emp-004` | `CREATE` | New employee + AI Capability Build |

**No DB writes** in Phase 1 — safe to run repeatedly.

---

## 2. Happy path — UPDATE (name + cohort swap)

`emp-001` in seed has `Senior Leadership` + `Managers Q2`. CSV replaces with `Senior Leadership` + `Engineering`.

```bat
curl -i -X POST http://localhost:8080/v1/upload/emp-with-cohort-assignment ^
  -H "X-Tenant-Id: vantage-fi" ^
  -H "X-User-Role: LND_ADMIN" ^
  -H "Idempotency-Key: curl-r2-update-001" ^
  -F "file=@samples/upload-update.csv"
```

**Expect:** `200 OK` — row 2: `"rowCategory":"UPDATE"`, operations include `UPDATE_EMPLOYEE`, `ADD_COHORT_MEMBERSHIP`, `REMOVE_COHORT_MEMBERSHIP`.

---

## 3. Row-level errors — partial preview

One file exercises duplicate id, unknown cohort, and email conflict.

```bat
curl -i -X POST http://localhost:8080/v1/upload/emp-with-cohort-assignment ^
  -H "X-Tenant-Id: vantage-fi" ^
  -H "X-User-Role: LND_ADMIN" ^
  -H "Idempotency-Key: curl-r2-errors-001" ^
  -F "file=@samples/upload-errors.csv"
```

**Expect:** `400 Bad Request` — top-level `"valid":false`, but full `"rows"` array is still returned.

| Row | Issue | Error code |
|-----|-------|------------|
| 2–3 | Same `emp-010` twice | `DUPLICATE_EMPLOYEE` |
| 4 | Cohort name not in catalog | `UNKNOWN_COHORT` |
| 5 | Email change on existing `emp-001` | `EMAIL_CONFLICT` |

---

## 4. File / auth errors

### Non-admin role (403)

```bat
curl -i -X POST http://localhost:8080/v1/upload/emp-with-cohort-assignment ^
  -H "X-Tenant-Id: vantage-fi" ^
  -H "X-User-Role: EMPLOYEE" ^
  -H "Idempotency-Key: curl-r2-forbidden-001" ^
  -F "file=@samples/upload-happy.csv"
```

**Expect:** `403` — `"Access Denied: Only LND_ADMIN can upload employee files."`

---

### Missing tenant header (400)

```bat
curl -i -X POST http://localhost:8080/v1/upload/emp-with-cohort-assignment ^
  -H "X-User-Role: LND_ADMIN" ^
  -H "Idempotency-Key: curl-r2-notenant-001" ^
  -F "file=@samples/upload-happy.csv"
```

**Expect:** `400` — `"X-Tenant-Id header is required."`

---

### Empty file (400)

Create an empty `samples/empty.csv` or use `-F "file=@samples/empty.csv"` after creating one:

```bat
curl -i -X POST http://localhost:8080/v1/upload/emp-with-cohort-assignment ^
  -H "X-Tenant-Id: vantage-fi" ^
  -H "X-User-Role: LND_ADMIN" ^
  -H "Idempotency-Key: curl-r2-empty-001" ^
  -F "file=@samples/empty.csv"
```

**Expect:** `400` — file empty error.

---

### Wrong extension — not `.csv` (400)

Save any text as `samples/not-csv.txt` or rename for the test:

```bat
curl -i -X POST http://localhost:8080/v1/upload/emp-with-cohort-assignment ^
  -H "X-Tenant-Id: vantage-fi" ^
  -H "X-User-Role: LND_ADMIN" ^
  -H "Idempotency-Key: curl-r2-ext-001" ^
  -F "file=@samples/not-csv.txt"
```

**Expect:** `400` — `"Only .csv files are allowed."`

---

## 5. Suggested run order (fresh app restart)

Run after `run.bat` on a clean H2 database:

```
§1  upload-happy.csv     → 200, valid true
§2  upload-update.csv    → 200, UPDATE row
§3  upload-errors.csv    → 400, valid false (inspect rows[])
§4  wrong role → empty file → wrong extension → missing tenant
```

Automated coverage: `FileUploadServiceReconciliationTests`, `FileUploadIntegrationTests`, and `test.bat`.

---

## Sample file reference

| File | Purpose |
|------|---------|
| [`samples/upload-happy.csv`](samples/upload-happy.csv) | CREATE + SKIP_OP — all valid |
| [`samples/upload-update.csv`](samples/upload-update.csv) | Name change + cohort ADD/REMOVE |
| [`samples/upload-errors.csv`](samples/upload-errors.csv) | Duplicate id, unknown cohort, email conflict |

**Note:** `cohort_names` uses **semicolons**, not commas. Example: `Senior Leadership;Managers Q2`.

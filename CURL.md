# curl command reference

Start the API first: `run.bat` (listens on `http://localhost:8080`).

**Tenant used below:** `vantage-fi` (BUCKET_02, max 3 sessions; topic 1 has 2 allocated slots in seed data).

**Date rule:** every `POST /v1/sessions` body uses `"date":"2026-08-01"` — must stay ≥ 14 days ahead of today; change if your clock has moved past that.

**Windows:** commands use `^` line breaks. **Mac/Linux:** replace `^` with `\`.

**Tip:** add `-i` to any command to print the HTTP status line (e.g. `201 Created`, `400 Bad Request`).

---

## 1. Catalogue

### List all 8 topics (global, no tenant header)

Returns the shared topic catalogue from `data.sql`.

```bat
curl -i http://localhost:8080/v1/topics
```

**Expect:** `200` — JSON array with ids 1–8.

---

### List every speaker (global, no filter)

Returns all speakers in the roster.

```bat
curl -i http://localhost:8080/v1/speakers
```

**Expect:** `200` — 3 speakers (Dr. Sarah Jenkins, Prof. Alex Rivera, Elena Rostova).

---

### List speakers who can deliver topic 1 (AI Strategy)

Filters via `speaker_topic` join — only eligible speakers for that topic.

```bat
curl -i "http://localhost:8080/v1/speakers?topic=1"
```

**Expect:** `200` — speaker ids **1** and **3** (both mapped to topic 1 in seed data).

---

## 2. Allocations

### Read vantage-fi topic slot plan

Shows how many sessions L&D allocated per topic (seed: topic 1 → 2 slots).

```bat
curl -i -H "X-Tenant-Id: vantage-fi" http://localhost:8080/v1/tenants/vantage-fi/allocations
```

**Expect:** `200` — `[{"tenantId":"vantage-fi","topicId":1,"allocatedSlots":2,...}]`.

---

### Replace vantage-fi allocations (2 + 1 = 3, equals bucket cap)

L&D redistributes slots; total allocated (3) must not exceed BUCKET_02 max (3).

```bat
curl -i -X PUT http://localhost:8080/v1/tenants/vantage-fi/allocations ^
  -H "Content-Type: application/json" ^
  -H "X-Tenant-Id: vantage-fi" ^
  -H "X-User-Role: LND_ADMIN" ^
  -d "[{\"topicId\":1,\"allocatedSlots\":2},{\"topicId\":2,\"allocatedSlots\":1}]"
```

**Expect:** `200` — full allocation list for the tenant after update.

---

### PUT over bucket cap (should fail)

Requests 4 slots total on a BUCKET_02 tenant (max 3).

```bat
curl -i -X PUT http://localhost:8080/v1/tenants/vantage-fi/allocations ^
  -H "Content-Type: application/json" ^
  -H "X-Tenant-Id: vantage-fi" ^
  -H "X-User-Role: LND_ADMIN" ^
  -d "[{\"topicId\":1,\"allocatedSlots\":2},{\"topicId\":2,\"allocatedSlots\":2}]"
```

**Expect:** `400` — `"exceeds bucket cap"`.

---

## 3. Schedule, list, fetch

### Schedule session — all employees (`audienceAll: true`)

Books topic 1 / speaker 1 on 2026-08-01; consumes one topic-1 slot and one global slot.

```bat
curl -i -X POST http://localhost:8080/v1/sessions ^
  -H "Content-Type: application/json" ^
  -H "X-Tenant-Id: vantage-fi" ^
  -H "X-User-Role: LND_ADMIN" ^
  -H "Idempotency-Key: curl-demo-001" ^
  -d "{\"topicId\":1,\"speakerId\":1,\"date\":\"2026-08-01\",\"title\":\"AI Workshop\",\"theme\":\"Leadership\",\"audienceAll\":true,\"cohortIds\":[]}"
```

**Expect:** `201 Created` — body includes `"id":1` (or next id). **Save that `id`.**

---

### List all sessions for vantage-fi

Tenant-scoped list; only sessions where `tenant_id = vantage-fi`.

```bat
curl -i -H "X-Tenant-Id: vantage-fi" http://localhost:8080/v1/sessions
```

**Expect:** `200` — array containing the session created above.

---

### Fetch one session by id

Replace `1` with the `id` from the POST response.

```bat
curl -i -H "X-Tenant-Id: vantage-fi" http://localhost:8080/v1/sessions/1
```

**Expect:** `200` — single session JSON with matching `id`, `topicId`, `speakerId`.

---

## 4. Idempotency

### Retry same schedule request (same Idempotency-Key)

Identical POST to §3 — must return the original session, not create a duplicate row.

```bat
curl -i -X POST http://localhost:8080/v1/sessions ^
  -H "Content-Type: application/json" ^
  -H "X-Tenant-Id: vantage-fi" ^
  -H "X-User-Role: LND_ADMIN" ^
  -H "Idempotency-Key: curl-demo-001" ^
  -d "{\"topicId\":1,\"speakerId\":1,\"date\":\"2026-08-01\",\"title\":\"AI Workshop\",\"theme\":\"Leadership\",\"audienceAll\":true,\"cohortIds\":[]}"
```

**Expect:** `200 OK` — same `"id"` as first POST; `GET /v1/sessions` still shows one session for that key.

---

### Second session with a new Idempotency-Key

Creates a separate booking (topic 1 still has a second allocated slot).

```bat
curl -i -X POST http://localhost:8080/v1/sessions ^
  -H "Content-Type: application/json" ^
  -H "X-Tenant-Id: vantage-fi" ^
  -H "X-User-Role: LND_ADMIN" ^
  -H "Idempotency-Key: curl-demo-002" ^
  -d "{\"topicId\":1,\"speakerId\":3,\"date\":\"2026-08-15\",\"title\":\"AI Deep Dive\",\"theme\":\"Strategy\",\"audienceAll\":true,\"cohortIds\":[]}"
```

**Expect:** `201` — new `id`; list endpoint now shows 2 sessions.

---

## 5. Cancel and re-book

### Cancel session id 1

Removes the booking; topic 1 allocation count stays at 2 (slot reopens).

```bat
curl -i -X DELETE http://localhost:8080/v1/sessions/1 ^
  -H "X-Tenant-Id: vantage-fi" ^
  -H "X-User-Role: LND_ADMIN"
```

**Expect:** `204 No Content` — empty body.

---

### Confirm session 1 is gone

```bat
curl -i -H "X-Tenant-Id: vantage-fi" http://localhost:8080/v1/sessions/1
```

**Expect:** `404` — `{"error":"Session not found."}`.

---

### Schedule again on topic 1 after cancel

Proves allocation was preserved — new booking should succeed.

```bat
curl -i -X POST http://localhost:8080/v1/sessions ^
  -H "Content-Type: application/json" ^
  -H "X-Tenant-Id: vantage-fi" ^
  -H "X-User-Role: LND_ADMIN" ^
  -H "Idempotency-Key: curl-demo-003" ^
  -d "{\"topicId\":1,\"speakerId\":1,\"date\":\"2026-08-20\",\"title\":\"AI Refresher\",\"theme\":\"Leadership\",\"audienceAll\":true,\"cohortIds\":[]}"
```

**Expect:** `201` — new session created on topic 1.

---

## 6. Cohort invitations

### Schedule for cohort `leadership-2026` only

`emp-001` belongs to that cohort; `emp-002` does not.

```bat
curl -i -X POST http://localhost:8080/v1/sessions ^
  -H "Content-Type: application/json" ^
  -H "X-Tenant-Id: vantage-fi" ^
  -H "X-User-Role: LND_ADMIN" ^
  -H "Idempotency-Key: curl-demo-cohort" ^
  -d "{\"topicId\":1,\"speakerId\":1,\"date\":\"2026-08-25\",\"title\":\"Leadership Cohort Session\",\"theme\":\"Crisis\",\"audienceAll\":false,\"cohortIds\":[\"leadership-2026\"]}"
```

**Expect:** `201` — `audienceAll:false`, `cohortIds:["leadership-2026"]`.

---

### emp-001 upcoming sessions (should include cohort session)

Resolves employee cohorts → merges audience-all + matching cohort-targeted sessions.

```bat
curl -i -H "X-Tenant-Id: vantage-fi" http://localhost:8080/v1/employees/emp-001/upcoming-sessions
```

**Expect:** `200` — array includes the cohort-targeted session (and any `audienceAll:true` sessions with `sessionDate >= today`).

---

### emp-002 upcoming sessions (should exclude cohort-only session)

`emp-002` is only in `engineering` — not invited to `leadership-2026` sessions.

```bat
curl -i -H "X-Tenant-Id: vantage-fi" http://localhost:8080/v1/employees/emp-002/upcoming-sessions
```

**Expect:** `200` — no cohort-only session from previous POST; may still list `audienceAll:true` sessions.

---

## 7. Multi-tenancy boundaries

### Missing X-Tenant-Id on sessions list

```bat
curl -i http://localhost:8080/v1/sessions
```

**Expect:** `400` — `"X-Tenant-Id header is required."`

---

### URL tenant ≠ header tenant on allocations

Path says `vantage-fi` but header says `apex-edu`.

```bat
curl -i -H "X-Tenant-Id: apex-edu" http://localhost:8080/v1/tenants/vantage-fi/allocations
```

**Expect:** `403` — header must match path tenant.

---

### Read session 1 as apex-edu (session belongs to vantage-fi)

```bat
curl -i -H "X-Tenant-Id: apex-edu" http://localhost:8080/v1/sessions/1
```

**Expect:** `404` — tenant cannot see another tenant's session.

---

## 8. Scheduling rule violations

### Speaker 2 cannot deliver topic 1 (not in speaker_topic)

```bat
curl -i -X POST http://localhost:8080/v1/sessions ^
  -H "Content-Type: application/json" ^
  -H "X-Tenant-Id: vantage-fi" ^
  -H "X-User-Role: LND_ADMIN" ^
  -H "Idempotency-Key: curl-bad-speaker" ^
  -d "{\"topicId\":1,\"speakerId\":2,\"date\":\"2026-08-01\",\"title\":\"Bad Pair\",\"theme\":\"X\",\"audienceAll\":true,\"cohortIds\":[]}"
```

**Expect:** `400` — speaker not eligible for topic.

---

### Date fewer than 14 days ahead

```bat
curl -i -X POST http://localhost:8080/v1/sessions ^
  -H "Content-Type: application/json" ^
  -H "X-Tenant-Id: vantage-fi" ^
  -H "X-User-Role: LND_ADMIN" ^
  -H "Idempotency-Key: curl-bad-date" ^
  -d "{\"topicId\":1,\"speakerId\":1,\"date\":\"2026-06-10\",\"title\":\"Too Soon\",\"theme\":\"X\",\"audienceAll\":true,\"cohortIds\":[]}"
```

**Expect:** `400` — must be at least 14 days in the future (adjust date if this is already in the past).

---

### audienceAll true AND cohortIds populated (XOR violation)

```bat
curl -i -X POST http://localhost:8080/v1/sessions ^
  -H "Content-Type: application/json" ^
  -H "X-Tenant-Id: vantage-fi" ^
  -H "X-User-Role: LND_ADMIN" ^
  -H "Idempotency-Key: curl-bad-audience" ^
  -d "{\"topicId\":1,\"speakerId\":1,\"date\":\"2026-08-01\",\"title\":\"Bad Audience\",\"theme\":\"X\",\"audienceAll\":true,\"cohortIds\":[\"leadership-2026\"]}"
```

**Expect:** `400` — audience must be all **or** cohorts, not both.

---

### audienceAll false with empty cohortIds (XOR violation)

```bat
curl -i -X POST http://localhost:8080/v1/sessions ^
  -H "Content-Type: application/json" ^
  -H "X-Tenant-Id: vantage-fi" ^
  -H "X-User-Role: LND_ADMIN" ^
  -H "Idempotency-Key: curl-bad-empty" ^
  -d "{\"topicId\":1,\"speakerId\":1,\"date\":\"2026-08-01\",\"title\":\"No Audience\",\"theme\":\"X\",\"audienceAll\":false,\"cohortIds\":[]}"
```

**Expect:** `400` — audience must be all **or** cohorts, not neither.

---

### Non-admin role blocked from scheduling

```bat
curl -i -X POST http://localhost:8080/v1/sessions ^
  -H "Content-Type: application/json" ^
  -H "X-Tenant-Id: vantage-fi" ^
  -H "X-User-Role: EMPLOYEE" ^
  -H "Idempotency-Key: curl-bad-role" ^
  -d "{\"topicId\":1,\"speakerId\":1,\"date\":\"2026-08-01\",\"title\":\"Wrong Role\",\"theme\":\"X\",\"audienceAll\":true,\"cohortIds\":[]}"
```

**Expect:** `403` — only `LND_ADMIN` can schedule.

---

## 9. Suggested run order (fresh app restart)

Run after `run.bat` on a clean H2 database so ids start at 1:

```
§1  topics → speakers → speakers?topic=1
§2  GET allocations → PUT allocations (valid)
§3  POST session (curl-demo-001) → GET list → GET by id
§4  POST retry (curl-demo-001) → POST second (curl-demo-002)
§5  DELETE id 1 → GET id 1 (404) → POST (curl-demo-003)
§6  POST cohort session → GET emp-001 → GET emp-002
§7–§8  error cases (any order)
```

Concurrent slot race and automated cap tests are covered by `test.bat` (not practical to reproduce with sequential curl).
